package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.Serializable;
import java.util.List;

import static org.telegram.bot.utils.DateUtils.formatTime;
import static org.telegram.bot.utils.DateUtils.deltaDatesToString;
import static org.telegram.bot.utils.TextUtils.withCapital;

@Component
@AllArgsConstructor
public class Weather implements CommandParent<SendMessage> {

    private final Logger log = LoggerFactory.getLogger(Weather.class);

    private final PropertiesConfig propertiesConfig;
    private final UserService userService;
    private final ChatService chatService;
    private final UserCityService userCityService;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    @Override
    public SendMessage parse(Update update) throws Exception {
        String token = propertiesConfig.getOpenweathermapId();
        if (token == null || token.equals("")) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        Message message = getMessageFromUpdate(update);
        Integer userId = message.getFrom().getId();
        String cityName;
        String responseText;

        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        if (textMessage == null) {
            User user = userService.get(userId);
            UserCity userCity = userCityService.get(user, chatService.get(message.getChatId()));
            if (userCity == null) {
                commandWaitingService.add(message, this.getClass());

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(message.getChatId().toString());
                sendMessage.setReplyToMessageId(message.getMessageId());
                sendMessage.setText("теперь напиши мне город, в котором надо посмотреть погоду");

                return sendMessage;
            } else {
                cityName = userCity.getCity().getNameEn();
            }
        } else {
            cityName = textMessage;
        }

        WeatherCurrent weatherCurrent = getWeatherCurrent(token, cityName);
        WeatherForecast weatherForecast = getWeatherForecast(token, cityName);

        responseText = prepareCurrentWeatherText(weatherCurrent) + prepareForecastWeatherText(weatherForecast);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private WeatherCurrent getWeatherCurrent(String token, String cityName) throws BotException {
        final String WEATHER_API_URL = "http://api.openweathermap.org/data/2.5/weather?lang=ru&units=metric&appid=" + token + "&q=";
        ResponseEntity<WeatherCurrent> response;

        try {
            response = botRestTemplate.getForEntity(WEATHER_API_URL + cityName, WeatherCurrent.class);
        } catch (RestClientException e) {
            throw new BotException("Ответ сервиса погоды: " + getErrorMessage(e));
        }

        return response.getBody();
    }

    private WeatherForecast getWeatherForecast(String token, String cityName) throws BotException {
        final String FORECAST_API_URL = "https://api.openweathermap.org/data/2.5/forecast?lang=ru&units=metric&cnt=6&appid=" + token + "&q=";
        ResponseEntity<WeatherForecast> response;

        try {
            response = botRestTemplate.getForEntity(FORECAST_API_URL + cityName, WeatherForecast.class);
        } catch (RestClientException e) {
            throw new BotException("Ответ сервиса погоды: " + getErrorMessage(e));
        }

        return response.getBody();
    }

    private String getErrorMessage(Exception e) {
        String errorText = e.getMessage();
        String responseText;

        if (errorText == null) {
            return speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
        }

        int i = errorText.indexOf("{");
        if (i < 0) {
            responseText = errorText;
        } else {
            ObjectMapper objectMapper = new ObjectMapper();
            WeatherError weatherError;
            try {
                weatherError = objectMapper.readValue(errorText.substring(i, errorText.length() - 1), WeatherError.class);
            } catch (JsonProcessingException jsonMappingException) {
                return speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
            }
            responseText = weatherError.getMessage();
        }

        return responseText;
    }

    private String prepareCurrentWeatherText(WeatherCurrent weatherCurrent) {
        StringBuilder buf = new StringBuilder();
        Sys sys = weatherCurrent.getSys();
        WeatherData weather = weatherCurrent.getWeather().get(0);
        Main main = weatherCurrent.getMain();
        Wind wind = weatherCurrent.getWind();

        buf.append("*").append(weatherCurrent.getName()).append("*(").append(sys.getCountry()).append(")\n```\n");
        buf.append(withCapital(weather.getDescription())).append(getWeatherEmoji(weather.getId())).append("\n");
        Rain rain = weatherCurrent.getRain();
        Snow snow = weatherCurrent.getSnow();
        if (rain != null) {
            String precipitations = getPrecipitations(rain, 1, true);
            if (precipitations != null) {
                buf.append(precipitations).append("\n");
            }

            precipitations = getPrecipitations(rain, 3, true);
            if (precipitations != null) {
                buf.append(precipitations).append("\n");
            }
        }
        if (snow != null) {
            String precipitations = getPrecipitations(snow, 1, false);
            if (precipitations != null) {
                buf.append(precipitations).append("\n");
            }

            precipitations = getPrecipitations(snow, 3, false);
            if (precipitations != null) {
                buf.append(precipitations).append("\n");
            }
        }
        buf.append("Температура:  ").append(String.format("%+.2f", main.getTemp())).append("°\n");
        buf.append("Ощущается:    ").append(String.format("%+.2f", main.getFeelsLike())).append("°\n");
        buf.append("Влажность:    ").append(main.getHumidity().intValue()).append("%\n");
        buf.append("Ветер:        ").append(wind.getSpeed()).append(" м/с ").append(getWindDirectionEmoji(wind.getDeg())).append("\n");
        Double gust = wind.getGust();
        if (gust != null) {
            buf.append("Порывы:       ").append(gust).append(" м/с ").append("\n");
        }
        buf.append("Облачность:   ").append(weatherCurrent.getClouds().getAll()).append("%\n");
        buf.append("Видимость:    ").append(weatherCurrent.getVisibility() / 1000).append(" км.\n");
        buf.append("Давление:     ").append(main.getPressure().intValue() * 0.75).append(" мм рт.ст. \n");
        buf.append("Восход:       ").append(formatTime(sys.getSunrise() + weatherCurrent.getTimezone())).append("\n");
        buf.append("Закат:        ").append(formatTime(sys.getSunset() + weatherCurrent.getTimezone())).append("\n");
        buf.append("Долгота дня:  ").append(deltaDatesToString((sys.getSunset() - sys.getSunrise()) * 1000L)).append("\n");
        buf.append("По состоянию: ").append(formatTime(weatherCurrent.getDt() + weatherCurrent.getTimezone())).append("\n");
        buf.append("```");

        return buf.toString();
    }

    private String prepareForecastWeatherText(WeatherForecast weatherForecast) {
        Integer timezone = weatherForecast.getCity().getTimezone();

        StringBuilder buf = new StringBuilder("*Прогноз по часам:*\n```\n");

        int maxLenghtOfTemp = weatherForecast.getList()
                .stream()
                .mapToInt(data -> String.format("%+.2f", data.getMain().getTemp()).length())
                .max()
                .orElse(5) + 2;

        weatherForecast.getList()
                .forEach(forecast -> buf.append(formatTime(forecast.getDt() + timezone), 0, 2).append(" ")
                    .append(getWeatherEmoji(forecast.getWeather().get(0).getId())).append(" ")
                    .append(String.format("%-" + maxLenghtOfTemp + "s", String.format("%+.2f", forecast.getMain().getTemp()) + "°"))
                    .append(String.format("%-4s", forecast.getMain().getHumidity().intValue() + "% "))
                    .append(forecast.getWind().getSpeed()).append("м/c")
                    .append("\n"));

        return buf.toString() + "```";
    }

    private String getWindDirectionEmoji(Integer degree) {
        if (degree == null) {
            return "";
        }

        String[] directions = {
                Emoji.DOWN_ARROW.getEmoji(), // с севера from north
                Emoji.DOWN_LEFT_ARROW.getEmoji(),
                Emoji.LEFT_ARROW.getEmoji(), //с востока from east
                Emoji.UP_LEFT_ARROW.getEmoji(),
                Emoji.UP_ARROW.getEmoji(), //с юга from south
                Emoji.UP_RIGHT_ARROW.getEmoji(),
                Emoji.RIGHT_ARROW.getEmoji(), //с запада from west
                Emoji.DOWN_RIGHT_ARROW.getEmoji()
        };

        return directions[ (int)Math.round((  ((double) degree % 360) / 45)) % 8 ];
    }

    private String getWeatherEmoji(Integer weatherId) {
        if (weatherId >= 200 && weatherId < 300) {
            return Emoji.ZAP.getEmoji();
        } else if (weatherId >= 300 && weatherId < 400) {
            return Emoji.UMBRELLA.getEmoji();
        } else if (weatherId >= 500 && weatherId < 600) {
            return Emoji.UMBRELLA_WITH_RAIN_DROPS.getEmoji();
        } else if (weatherId >= 600 && weatherId < 700) {
            return Emoji.SNOWFLAKE.getEmoji();
        } else if (weatherId.equals(701) || weatherId.equals(741)) {
            return "\uD83C\uDF2B";
        } else if (weatherId.equals(800)) {
            return Emoji.SUNNY.getEmoji();
        } else if (weatherId.equals(801)) {
            return Emoji.WHITE_SUN_WITH_SMALL_CLOUD.getEmoji();
        } else if (weatherId.equals(802)) {
            return Emoji.SUN_BEHIND_CLOUD.getEmoji();
        } else if (weatherId.equals(803)) {
            return Emoji.SUN_BEHIND_LARGE_CLOUD.getEmoji();
        } else if (weatherId.equals(804)) {
            return Emoji.CLOUD.getEmoji();
        } else {
            return "";
        }
    }

    private String getPrecipitations(Precipitations precipitations, Integer hours, boolean rain) {
        String emoji;
        if (rain) {
            emoji = Emoji.DROPLET.getEmoji();
        } else {
            emoji = Emoji.SNOWFLAKE.getEmoji();
        }

        if (hours.equals(1)) {
            Double oneHour = precipitations.getOneHours();
            if (oneHour != null) {
                return emoji + "За час:     " + String.format("%.2f", oneHour) + " мм";
            }
        } else if (hours.equals(3)) {
            Double threeHours = precipitations.getThreeHours();
            if (threeHours != null) {
                return emoji + "За три часа:" + String.format("%.2f", threeHours) + " мм";
            }
        }

        return null;
    }

    @Data
    @NoArgsConstructor
    private static class WeatherForecast {
        private String cod;
        private Integer message;
        private Integer cnt;
        private List<WeatherForecastData> list;
        private Weather.City city;
    }

    @Data
    @NoArgsConstructor
    private static class WeatherForecastData {
        private Integer dt;
        private Main main;
        private List<WeatherData> weather;
        private Clouds clouds;
        private Wind wind;
        private Rain rain;
        private Snow snow;
        private Integer visibility;
        private Integer pop;
        @JsonIgnore
        private Sys sys;
        @JsonProperty("dt_txt")
        private String dtTxt;
    }

    @NoArgsConstructor
    private static class Rain extends Precipitations {}

    @NoArgsConstructor
    private static class Snow extends Precipitations {}

    @Data
    @NoArgsConstructor
    private static class Precipitations {
        @JsonProperty("1h")
        private Double oneHours;
        @JsonProperty("3h")
        private Double threeHours;
        @JsonProperty("6h")
        private Double sixHours;
    }

    @Data
    @NoArgsConstructor
    private static class City {
        private Integer id;
        private String name;
        private Coord coord;
        private String country;
        private Integer population;
        private Integer timezone;
        private Integer sunrise;
        private Integer sunset;
    }

    @Data
    @NoArgsConstructor
    private static class WeatherCurrent {
        private Coord coord;
        private List<WeatherData> weather;
        private String base;
        private Main main;
        private Double visibility;
        private Wind wind;
        private Clouds clouds;
        private Rain rain;
        private Snow snow;
        private Integer dt;
        private Sys sys;
        private Integer timezone;
        private Long id;
        private String name;
        private Integer cod;
    }

    @Data
    @NoArgsConstructor
    private static class Coord {
        private Double lon;
        private Double lat;
    }

    @Data
    @NoArgsConstructor
    private static class Main {
        private Double temp;

        @JsonProperty("feels_like")
        private Double feelsLike;

        private Double pressure;

        private Double humidity;

        @JsonProperty("temp_min")
        private Double tempMin;

        @JsonProperty("temp_max")
        private Double tempMax;

        @JsonProperty("sea_level")
        private Double seaLevel;

        @JsonProperty("grnd_level")
        private Double grndLevel;

        @JsonProperty("temp_kf")
        private Integer tempKf;
    }

    @Data
    @NoArgsConstructor
    private static class Wind {
        private Double speed;
        private Integer deg;
        private Double gust;
    }

    @Data
    @NoArgsConstructor
    private static class Clouds {
        private Integer all;
    }

    @Data
    @NoArgsConstructor
    private static class Sys {
        private Integer type;
        private Integer id;
        private Double message;
        private String country;
        private Integer sunrise;
        private Integer sunset;
    }

    @Data
    @NoArgsConstructor
    private static class WeatherData {
        private Integer id;
        private String main;
        private String description;
        private String icon;
    }

    @Data
    @NoArgsConstructor
    private static class WeatherError implements Serializable {
        private String cod;
        private String message;
    }
}
