package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.util.List;

import static org.telegram.bot.utils.DateUtils.*;
import static org.telegram.bot.utils.TextUtils.withCapital;

@Component
@RequiredArgsConstructor
@Slf4j
public class Weather implements CommandParent<SendMessage> {

    private final PropertiesConfig propertiesConfig;
    private final UserCityService userCityService;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    @Override
    public SendMessage parse(Update update) {
        String token = propertiesConfig.getOpenweathermapId();
        if (StringUtils.isEmpty(token)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        Message message = getMessageFromUpdate(update);
        Long userId = message.getFrom().getId();
        String cityName;
        String responseText;

        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        if (textMessage == null) {
            log.debug("Empty request. Searching for users city");
            UserCity userCity = userCityService.get(new User().setUserId(userId), new Chat().setChatId(message.getChatId()));
            if (userCity == null) {
                log.debug("City in not set. Turning on command waiting");
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

        log.debug("City name is {}", cityName);
        WeatherCurrent weatherCurrent = getWeatherCurrent(token, cityName);
        WeatherForecast weatherForecast = getWeatherForecast(token, cityName);

        responseText = prepareCurrentWeatherText(weatherCurrent)
                + prepareHourlyForecastWeatherText(weatherForecast)
                + prepareDailyForecastWeatherText(weatherForecast);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setDisableWebPagePreview(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    /**
     * Getting current weather data from service.
     *
     * @param token access token.
     * @param cityName name of city
     * @return current weather data.
     * @throws BotException if get an error from service.
     */
    private WeatherCurrent getWeatherCurrent(String token, String cityName) throws BotException {
        final String WEATHER_API_URL = "http://api.openweathermap.org/data/2.5/weather?lang=ru&units=metric&appid=" + token + "&q=";
        ResponseEntity<WeatherCurrent> response;

        try {
            response = botRestTemplate.getForEntity(WEATHER_API_URL + cityName, WeatherCurrent.class);
        } catch (HttpClientErrorException e) {
            throw new BotException("Ответ сервиса погоды: " + getErrorMessage(e));
        }

        return response.getBody();
    }

    /**
     * Getting weather forecast data from service.
     *
     * @param token access token.
     * @param cityName name of city
     * @return weather forecast data.
     * @throws BotException if get an error from service.
     */
    private WeatherForecast getWeatherForecast(String token, String cityName) throws BotException {
        final String FORECAST_API_URL = "https://api.openweathermap.org/data/2.5/forecast?lang=ru&units=metric&appid=" + token + "&q=";
        ResponseEntity<WeatherForecast> response;

        try {
            response = botRestTemplate.getForEntity(FORECAST_API_URL + cityName, WeatherForecast.class);
        } catch (HttpClientErrorException e) {
            throw new BotException("Ответ сервиса погоды: " + getErrorMessage(e));
        }

        return response.getBody();
    }

    /**
     * Getting error message by Exception from service.
     *
     * @param e exception from Rest client.
     * @return text of error message.
     */
    private String getErrorMessage(HttpClientErrorException e) {
        return new JSONObject(e.getResponseBodyAsString()).getString("message");
    }

    /**
     * Preparing current weather part of weather.
     *
     * @param weatherCurrent current weather data.
     * @return current weather info.
     */
    private String prepareCurrentWeatherText(WeatherCurrent weatherCurrent) {
        final String openWeatherMapWeatherUrl = "https://openweathermap.org/city/";
        StringBuilder buf = new StringBuilder();
        Sys sys = weatherCurrent.getSys();
        WeatherData weather = weatherCurrent.getWeather().get(0);
        Main main = weatherCurrent.getMain();
        Wind wind = weatherCurrent.getWind();

        buf.append("[").append(weatherCurrent.getName()).append("](" + openWeatherMapWeatherUrl).append(weatherCurrent.getId()).append(")(").append(sys.getCountry()).append(")\n```\n");
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
        buf.append("Температура:  ").append(String.format("%+.2f", main.getTemp())).append("°C\n");
        buf.append("Ощущается:    ").append(String.format("%+.2f", main.getFeelsLike())).append("°C\n");
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

    /**
     * Preparing hourly forecast part of weather.
     *
     * @param weatherForecast weather forecast data.
     * @return forecast info.
     */
    private String prepareHourlyForecastWeatherText(WeatherForecast weatherForecast) {
        final int hoursOfForecastCount = 6;
        Integer timezone = weatherForecast.getCity().getTimezone();

        StringBuilder buf = new StringBuilder("*Прогноз по часам:*\n```\n");

        int maxLengthOfTemp = weatherForecast.getList()
                .stream()
                .mapToInt(data -> String.format("%+.0f", data.getMain().getTemp()).length())
                .max()
                .orElse(5) + 2;

        weatherForecast.getList()
                .stream()
                .limit(hoursOfForecastCount)
                .forEach(forecast -> buf.append(formatTime(forecast.getDt() + timezone), 0, 2).append(" ")
                    .append(getWeatherEmoji(forecast.getWeather().get(0).getId())).append(" ")
                    .append(String.format("%-" + maxLengthOfTemp + "s", String.format("%+.0f", forecast.getMain().getTemp()) + "°"))
                    .append(String.format("%-4s", forecast.getMain().getHumidity().intValue() + "% "))
                    .append(String.format("%.0f", forecast.getWind().getSpeed())).append("м/c ")
                    .append("\n"));

        return buf + "```";
    }

    /**
     * Preparing daily forecast part of weather.
     *
     * @param weatherForecast weather forecast data.
     * @return forecast info.
     */
    private String prepareDailyForecastWeatherText(WeatherForecast weatherForecast) {
        Integer timezone = weatherForecast.getCity().getTimezone();

        StringBuilder buf = new StringBuilder("*Прогноз по дням:*\n```\n");

        LocalDate firstDateOfForecast = unixTimeToLocalDateTime(weatherForecast.getList().get(0).getDt() + timezone).toLocalDate();
        LocalDate lastDateOfForecast = firstDateOfForecast.plusDays(5);

        List<WeatherForecastData> forecastList = weatherForecast.getList();
        for (int i = 0; i < forecastList.size(); i++) {
            LocalDate currentDate = unixTimeToLocalDateTime(forecastList.get(i).getDt()).toLocalDate();

            if (currentDate.isAfter(firstDateOfForecast) && currentDate.getDayOfMonth() != lastDateOfForecast.getDayOfMonth()) {
                WeatherForecastData minTemp = forecastList.get(i);
                WeatherForecastData maxTemp = forecastList.get(i);

                for (int j = i; j < i + 9 && j < forecastList.size(); j++) {
                    WeatherForecastData currentForecast = forecastList.get(j);

                    if (currentForecast.getMain().getTemp() < minTemp.getMain().getTemp()) {
                        minTemp = currentForecast;
                    }

                    if (currentForecast.getMain().getTemp() > maxTemp.getMain().getTemp()) {
                        maxTemp = currentForecast;
                    }
                }

                buf.append(currentDate.getDayOfMonth()).append(" ").append(getDayOfWeek(currentDate)).append(" ")
                        .append(getWeatherEmoji(maxTemp.getWeather().get(0).getId())).append(" ")
                        .append(String.format("%+.0f", maxTemp.getMain().getTemp())).append("° ")
                        .append(getWeatherEmoji(minTemp.getWeather().get(0).getId())).append(" ")
                        .append(String.format("%+.0f", minTemp.getMain().getTemp())).append("° ").append("\n");

                firstDateOfForecast = currentDate;
                i = i + 8;
            }
        }

        return buf + "```";
    }

    /**
     * Getting emoji symbol of wind direction.
     *
     * @param degree of direction.
     * @return emoji symbol.
     */
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

    /**
     * Getting Emoji for weather.
     *
     * @param weatherId id of weather type.
     * @return emoji symbol.
     */
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

    /**
     * Getting precipitation string of weather.
     *
     * @param precipitations precipitations data.
     * @param hours count of hours.
     * @param rain rain?
     * @return precipitation info.
     */
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
    private static class WeatherForecast {
        private String cod;
        private Integer message;
        private Integer cnt;
        private List<WeatherForecastData> list;
        private Weather.City city;
    }

    @Data
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

    private static class Rain extends Precipitations {}

    private static class Snow extends Precipitations {}

    @Data
    private static class Precipitations {
        @JsonProperty("1h")
        private Double oneHours;
        @JsonProperty("3h")
        private Double threeHours;
        @JsonProperty("6h")
        private Double sixHours;
    }

    @Data
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
    private static class Coord {
        private Double lon;
        private Double lat;
    }

    @Data
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
    private static class Wind {
        private Double speed;
        private Integer deg;
        private Double gust;
    }

    @Data
    private static class Clouds {
        private Integer all;
    }

    @Data
    private static class Sys {
        private Integer type;
        private Integer id;
        private Double message;
        private String country;
        private Integer sunrise;
        private Integer sunset;
    }

    @Data
    private static class WeatherData {
        private Integer id;
        private String main;
        private String description;
        private String icon;
    }
}
