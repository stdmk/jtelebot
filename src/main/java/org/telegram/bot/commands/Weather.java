package org.telegram.bot.commands;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import org.telegram.bot.Bot;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.telegram.bot.utils.DateUtils.*;
import static org.telegram.bot.utils.TextUtils.isThatPositiveInteger;
import static org.telegram.bot.utils.TextUtils.withCapital;

@Component
@RequiredArgsConstructor
@Slf4j
public class Weather implements Command {

    private static final String OPEN_WEATHER_SITE_URL = "https://openweathermap.org/city/";
    private static final String CURRENT_WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather?lang=%s&units=metric&appid=%s&%s=";
    private static final String FORECAST_WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/forecast?lang=%s&units=metric&appid=%s&%s=";
    private static final String PRECIPITATION_CAPTION_FORMAT = "%-12s";
    private static final String WEATHER_CAPTION_FORMAT = "%-15s";
    private static final int HOURLY_FORECAST_LENGTH_OF_ADDITIONAL_SYMBOLS = 2;
    private static final int HOURLY_FORECAST_MIN_LENGTH_OF_TEMP = 2;
    private static final int HOURLY_FORECAST_HOURS_OF_FORECAST_COUNT = 8;
    private static final int DAILY_FORECAST_MINIMUM_REQUIRED_SPACE_COUNT = 3;

    private final Bot bot;
    private final PropertiesConfig propertiesConfig;
    private final UserCityService userCityService;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;
    private final InternationalizationService internationalizationService;
    private final LanguageResolver languageResolver;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        String token = propertiesConfig.getOpenweathermapId();
        if (StringUtils.isEmpty(token)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        User user = message.getUser();
        String cityName;
        String responseText;

        String commandArgument = commandWaitingService.getText(message);

        if (commandArgument == null) {
            log.debug("Empty request. Searching for users city");
            UserCity userCity = userCityService.get(user, new Chat().setChatId(message.getChatId()));
            if (userCity == null) {
                log.debug("City in not set. Turning on command waiting");
                commandWaitingService.add(message, this.getClass());
                return returnResponse(new TextResponse(message)
                        .setText("${command.weather.commandwaitingstart}"));
            } else {
                cityName = userCity.getCity().getNameEn();
            }
        } else {
            cityName = commandArgument;
        }
        log.debug("City name is {}", cityName);

        String languageCode = languageResolver.getChatLanguageCode(message, user);

        WeatherCurrent weatherCurrent = getWeatherCurrent(token, cityName, languageCode);
        WeatherForecast weatherForecast = getWeatherForecast(token, cityName, languageCode);
        normalizeWeatherForecast(weatherForecast);

        responseText = prepareCurrentWeatherText(weatherCurrent, languageCode)
                + prepareHourlyForecastWeatherText(weatherForecast)
                + prepareDailyForecastWeatherText(weatherForecast, languageCode);

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(new ResponseSettings()
                        .setFormattingStyle(FormattingStyle.MARKDOWN)
                        .setWebPagePreview(false)));
    }

    /**
     * Getting current weather data from service.
     *
     * @param token access token.
     * @param city name of city
     * @param lang user's language.
     * @return current weather data.
     * @throws BotException if get an error from service.
     */
    private WeatherCurrent getWeatherCurrent(String token, String city, String lang) throws BotException {
        String weatherApiUrl = String.format(CURRENT_WEATHER_API_URL, lang, token, getQueryParameter(city));

        ResponseEntity<WeatherCurrent> response;
        try {
            response = botRestTemplate.getForEntity(weatherApiUrl + city, WeatherCurrent.class);
        } catch (HttpClientErrorException e) {
            throw new BotException("${command.weather.apiresponse}: " + getErrorMessage(e));
        }

        return response.getBody();
    }

    /**
     * Getting weather forecast data from service.
     *
     * @param token access token.
     * @param city name of city
     * @param lang user's language.
     * @return weather forecast data.
     * @throws BotException if get an error from service.
     */
    private WeatherForecast getWeatherForecast(String token, String city, String lang) throws BotException {
        String forecastApiUrl = String.format(FORECAST_WEATHER_API_URL, lang, token, getQueryParameter(city));

        ResponseEntity<WeatherForecast> response;
        try {
            response = botRestTemplate.getForEntity(forecastApiUrl + city, WeatherForecast.class);
        } catch (HttpClientErrorException e) {
            throw new BotException("${command.weather.apiresponse}: " + getErrorMessage(e));
        }

        return response.getBody();
    }

    /**
     * Get the value of a query parameter to search for weather by city identifier or name.
     *
     * @param value input value.
     * @return query parameter value.
     */
    private String getQueryParameter(String value) {
        return isThatPositiveInteger(value) ? "id" : "q";
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
     * @param lang user's language.
     * @return current weather info.
     */
    private String prepareCurrentWeatherText(WeatherCurrent weatherCurrent, String lang) {
        StringBuilder buf = new StringBuilder();
        Sys sys = weatherCurrent.getSys();
        WeatherData weather = weatherCurrent.getWeather().get(0);
        Main main = weatherCurrent.getMain();
        Wind wind = weatherCurrent.getWind();

        buf.append("[").append(weatherCurrent.getName()).append("](" + OPEN_WEATHER_SITE_URL).append(weatherCurrent.getId()).append(")(")
                .append(sys.getCountry()).append(")\n");
        buf.append(withCapital(weather.getDescription())).append(getWeatherEmoji(weather.getId())).append("\n");
        Rain rain = weatherCurrent.getRain();
        Snow snow = weatherCurrent.getSnow();
        if (rain != null) {
            String precipitations = getPrecipitations(rain, 1, true, lang);
            if (precipitations != null) {
                buf.append("`").append(precipitations).append("`\n");
            }

            precipitations = getPrecipitations(rain, 3, true, lang);
            if (precipitations != null) {
                buf.append("`").append(precipitations).append("`\n");
            }
        }
        if (snow != null) {
            String precipitations = getPrecipitations(snow, 1, false, lang);
            if (precipitations != null) {
                buf.append("`").append(precipitations).append("`\n");
            }

            precipitations = getPrecipitations(snow, 3, false, lang);
            if (precipitations != null) {
                buf.append("`").append(precipitations).append("`\n");
            }
        }
        
        buf.append(buildWeatherItem("`${command.weather.temperature}", lang)).append(String.format("%+.2f", main.getTemp())).append("°C`\n");
        buf.append(buildWeatherItem("`${command.weather.feelslike}", lang)).append(String.format("%+.2f", main.getFeelsLike())).append("°C`\n");
        buf.append(buildWeatherItem("`${command.weather.humidity}", lang)).append(main.getHumidity().intValue()).append("%`\n");
        buf.append(buildWeatherItem("`${command.weather.wind}", lang)).append(wind.getSpeed()).append(" ${command.weather.meterspersecond} ").append(getWindDirectionEmoji(wind.getDeg())).append("`\n");
        Double gust = wind.getGust();
        if (gust != null) {
            buf.append(buildWeatherItem("`${command.weather.gusts}", lang)).append(gust).append(" ${command.weather.meterspersecond} ").append("`\n");
        }
        if (weatherCurrent.getClouds() != null) {
            buf.append(buildWeatherItem("`${command.weather.cloudy}", lang)).append(weatherCurrent.getClouds().getAll()).append("%`\n");
        }
        if (weatherCurrent.getVisibility() != null) {
            buf.append(buildWeatherItem("`${command.weather.visibility}", lang)).append(weatherCurrent.getVisibility() / 1000).append(" ${command.weather.kilometers}.`\n");
        }
        buf.append(buildWeatherItem("`${command.weather.pressure}", lang)).append(main.getPressure().intValue() * 0.75).append(" ${command.weather.mmhg}. `\n");
        buf.append(buildWeatherItem("`${command.weather.sunrise}", lang)).append(formatTime(sys.getSunrise() + weatherCurrent.getTimezone())).append("`\n");
        buf.append(buildWeatherItem("`${command.weather.sunset}", lang)).append(formatTime(sys.getSunset() + weatherCurrent.getTimezone())).append("`\n");
        buf.append(buildWeatherItem("`${command.weather.daylength}", lang)).append(durationToString((sys.getSunset() - sys.getSunrise()) * 1000L)).append("`\n");
        buf.append(buildWeatherItem("`${command.weather.asof}", lang)).append(formatTime(weatherCurrent.getDt() + weatherCurrent.getTimezone())).append("`\n");

        return buf.toString();
    }

    /**
     * Preparing hourly forecast part of weather.
     *
     * @param weatherForecast weather forecast data.
     * @return forecast info.
     */
    private String prepareHourlyForecastWeatherText(WeatherForecast weatherForecast) {
        Integer timezone = weatherForecast.getCity().getTimezone();
        StringBuilder buf = new StringBuilder("*${command.weather.hourlyforecast}:*\n");

        List<WeatherForecastData> weatherForecastList = weatherForecast.getList()
                .stream()
                .limit(HOURLY_FORECAST_HOURS_OF_FORECAST_COUNT)
                .toList();

        int maxLengthOfTemp = weatherForecastList
                .stream()
                .mapToInt(data -> String.format("%+.0f", data.getMain().getTemp()).length())
                .max()
                .orElse(HOURLY_FORECAST_MIN_LENGTH_OF_TEMP) + HOURLY_FORECAST_LENGTH_OF_ADDITIONAL_SYMBOLS;

        weatherForecastList
                .forEach(forecast -> buf.append("`").append(formatTime(forecast.getDt() + timezone), 0, 2).append(" ")
                    .append(getWeatherEmoji(forecast.getWeather().get(0).getId())).append(" ")
                    .append(String.format("%-" + maxLengthOfTemp + "s", String.format("%+.0f", forecast.getMain().getTemp()) + "°"))
                    .append(String.format("%-4s", forecast.getMain().getHumidity().intValue() + "% "))
                    .append(String.format("%.0f", forecast.getWind().getSpeed())).append("${command.weather.meterspersecond} ")
                    .append("`\n"));

        return buf.toString();
    }

    /**
     * Preparing daily forecast part of weather.
     *
     * @param weatherForecast weather forecast data.
     * @param lang user's language.
     * @return forecast info.
     */
    private String prepareDailyForecastWeatherText(WeatherForecast weatherForecast, String lang) {
        StringBuilder buf = new StringBuilder("*${command.weather.dailyforecast}:*\n");
        List<WeatherForecastData> forecastList = weatherForecast.getList();
        LocalDate firstDate = forecastList.get(0).getNormalizedDate().toLocalDate();

        List<LocalDate> dateOfForecast = Stream.of(1, 2, 3, 4).map(firstDate::plusDays).toList();
        List<List<WeatherForecastData>> forecastListList = dateOfForecast.stream().map(date -> getForecastDataByDate(forecastList, date)).toList();
        int spaceCount = getSpaceCount(forecastListList);

        Stream.of(0, 1, 2, 3).forEach(index ->
                buf.append(buildDailyForecastString(forecastListList.get(index), dateOfForecast.get(index), spaceCount, lang)));

        return buf.toString();
    }

    private List<WeatherForecastData> getForecastDataByDate(List<WeatherForecastData> forecastList, LocalDate date) {
        LocalDateTime dateTimeStart = date.atStartOfDay();
        LocalDateTime dateTimeEnd = date.atTime(LocalTime.MAX).plusSeconds(1);

        return forecastList
                .stream()
                .filter(weatherForecastData -> weatherForecastData.getNormalizedDate().isAfter(dateTimeStart)
                        && weatherForecastData.getNormalizedDate().isBefore(dateTimeEnd))
                .toList();
    }

    private String buildDailyForecastString(List<WeatherForecastData> forecastData, LocalDate date, int spaceCount, String lang) {
        WeatherForecastData max = forecastData
                .stream()
                .max(Comparator.comparing(weatherForecastData -> weatherForecastData.getMain().getTemp()))
                .orElse(forecastData.get(0));

        WeatherForecastData min = forecastData
                .stream()
                .min(Comparator.comparing(weatherForecastData -> weatherForecastData.getMain().getTemp()))
                .orElse(forecastData.get(0));

        return "`" + String.format("%02d", date.getDayOfMonth()) + " " + getDayOfWeek(date, lang) + " " +
                getWeatherEmoji(max.getWeather().get(0).getId()) + " " +
                String.format("%-" + spaceCount + "s", String.format("%+.0f", max.getMain().getTemp()) + "°") +
                getWeatherEmoji(min.getWeather().get(0).getId()) + " " +
                String.format("%+.0f", min.getMain().getTemp()) + "°" + "`\n";
    }

    private int getSpaceCount(List<List<WeatherForecastData>> forecastListList) {
        Long maxTempValueAbs = forecastListList
                .stream()
                .map(forecastList -> forecastList.stream().map(WeatherForecastData::getMain).toList())
                .map(mainList -> mainList.stream().map(Main::getTemp).toList())
                .map(temps -> temps.stream().max(Double::compareTo).orElse(0D))
                .map(Math::round)
                .map(Math::abs)
                .max(Long::compareTo)
                .orElse(1L);

        return DAILY_FORECAST_MINIMUM_REQUIRED_SPACE_COUNT + String.valueOf(maxTempValueAbs).length();
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
                Emoji.DOWN_ARROW.getSymbol(), // с севера from north
                Emoji.DOWN_LEFT_ARROW.getSymbol(),
                Emoji.LEFT_ARROW.getSymbol(), //с востока from east
                Emoji.UP_LEFT_ARROW.getSymbol(),
                Emoji.UP_ARROW.getSymbol(), //с юга from south
                Emoji.UP_RIGHT_ARROW.getSymbol(),
                Emoji.RIGHT_ARROW.getSymbol(), //с запада from west
                Emoji.DOWN_RIGHT_ARROW.getSymbol()
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
            return Emoji.ZAP.getSymbol();
        } else if (weatherId >= 300 && weatherId < 400) {
            return Emoji.UMBRELLA.getSymbol();
        } else if (weatherId >= 500 && weatherId < 600) {
            return Emoji.UMBRELLA_WITH_RAIN_DROPS.getSymbol();
        } else if (weatherId >= 600 && weatherId < 700) {
            return Emoji.SNOWFLAKE.getSymbol();
        } else if (weatherId.equals(701) || weatherId.equals(741)) {
            return "\uD83C\uDF2B";
        } else if (weatherId.equals(800)) {
            return Emoji.SUNNY.getSymbol();
        } else if (weatherId.equals(801)) {
            return Emoji.WHITE_SUN_WITH_SMALL_CLOUD.getSymbol();
        } else if (weatherId.equals(802)) {
            return Emoji.SUN_BEHIND_CLOUD.getSymbol();
        } else if (weatherId.equals(803)) {
            return Emoji.SUN_BEHIND_LARGE_CLOUD.getSymbol();
        } else if (weatherId.equals(804)) {
            return Emoji.CLOUD.getSymbol();
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
    private String getPrecipitations(Precipitations precipitations, Integer hours, boolean rain, String lang) {
        String emoji;
        if (rain) {
            emoji = Emoji.DROPLET.getSymbol();
        } else {
            emoji = Emoji.SNOWFLAKE.getSymbol();
        }

        if (hours.equals(1)) {
            Double oneHour = precipitations.getOneHours();
            if (oneHour != null) {
                return emoji + buildWeatherItem("${command.weather.perhour}", lang, PRECIPITATION_CAPTION_FORMAT) + String.format("%.2f", oneHour) + " мм";
            }
        } else if (hours.equals(3)) {
            Double threeHours = precipitations.getThreeHours();
            if (threeHours != null) {
                return emoji + buildWeatherItem("${command.weather.threehours}", lang, PRECIPITATION_CAPTION_FORMAT) + String.format("%.2f", threeHours) + " мм";
            }
        }

        return null;
    }

    private String buildWeatherItem(String placeholder, String lang) {
        return buildWeatherItem(placeholder, lang, WEATHER_CAPTION_FORMAT);
    }

    private String buildWeatherItem(String placeholder, String lang, String captionFormat) {
        String internalizedText = internationalizationService.internationalize(placeholder, lang);
        return String.format(captionFormat, internalizedText + ":");
    }

    private void normalizeWeatherForecast(WeatherForecast weatherForecast) {
        Integer timezone = weatherForecast.getCity().getTimezone();
        weatherForecast.getList().forEach(weatherForecastData ->
                weatherForecastData.setNormalizedDate(getDateTimeFromWeatherForecastData(weatherForecastData, timezone)));
    }

    private LocalDateTime getDateTimeFromWeatherForecastData(WeatherForecastData weatherForecastData, long timezone) {
        return unixTimeToLocalDateTimeUtc(weatherForecastData.getDt() + timezone);
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
    @JsonIgnoreProperties(ignoreUnknown = true)
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
        private LocalDateTime normalizedDate;
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
