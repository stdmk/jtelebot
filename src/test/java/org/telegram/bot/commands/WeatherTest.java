package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.entities.City;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherTest {

    @Mock
    private Bot bot;
    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private UserCityService userCityService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private InternationalizationService internationalizationService;
    @Mock
    private LanguageResolver languageResolver;

    @Mock
    private ResponseEntity<Weather.WeatherCurrent> weatherCurrentResponseEntity;
    @Mock
    private ResponseEntity<Weather.WeatherForecast> weatherForecastResponseEntity;

    @InjectMocks
    private Weather weather;

    @Test
    void parseWithoutTokenTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("weather");

        when(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> weather.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());
        verify(bot, never()).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithoutArgumentAndCitySetTest() {
        final String expectedResponseText = "${command.weather.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup("weather");
        Message message = request.getMessage();

        when(propertiesConfig.getOpenweathermapId()).thenReturn("token");
        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());

        BotResponse botResponse = weather.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(commandWaitingService).add(message, Weather.class);
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithoutArgumentButCitySetAndApiExceptionTest() {
        final String expectedErrorText = "error";
        final String token = "token";
        final String cityName = "city";
        final String lang = "en";
        final String expectedApiUrl = "https://api.openweathermap.org/data/2.5/weather?lang=" + lang + "&units=metric&appid=" + token + "&q=" + cityName;
        final String apiResponse = "{\"message\":\"" + expectedErrorText + "\"}";
        BotRequest request = TestUtils.getRequestFromGroup("weather");
        Message message = request.getMessage();

        when(propertiesConfig.getOpenweathermapId()).thenReturn(token);
        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        City city = new City();
        city.setNameEn(cityName);
        UserCity userCity = new UserCity();
        userCity.setCity(city);
        when(userCityService.get(message.getUser(), message.getChat())).thenReturn(userCity);
        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(lang);
        when(botRestTemplate.getForEntity(expectedApiUrl, Weather.WeatherCurrent.class))
                .thenThrow(new HttpClientErrorException(
                        HttpStatus.BAD_REQUEST,
                        "bad request",
                        apiResponse.getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8));

        BotException botException = assertThrows(BotException.class, () -> weather.parse(request));
        assertEquals("${command.weather.apiresponse}: " + expectedErrorText, botException.getMessage());
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseTest() {
        final String expectedResponseText = """
                [cityName](https://openweathermap.org/city/123)(ru)
                Desc☂\uFE0F
                `\uD83D\uDCA7${command.weather.perhour}:28,12 мм`
                `\uD83D\uDCA7${command.weather.threehours}:29,12 мм`
                `❄${command.weather.perhour}:31,12 мм`
                `❄${command.weather.threehours}:32,12 мм`
                `${command.weather.temperature}:+0,12°C`
                `${command.weather.feelslike}:+1,12°C`
                `${command.weather.humidity}:70%`
                `${command.weather.wind}:27.123 ${command.weather.meterspersecond} ↙\uFE0F`
                `${command.weather.gusts}:26.123 ${command.weather.meterspersecond} `
                `${command.weather.cloudy}:1001%`
                `${command.weather.visibility}:1.000123 ${command.weather.kilometers}.`
                `${command.weather.pressure}:557.25 ${command.weather.mmhg}. `
                `${command.weather.sunrise}:06:13:03`
                `${command.weather.sunset}:13:07:03`
                `${command.weather.daylength}:6 ${utils.date.h}. 54 ${utils.date.m}. `
                `${command.weather.asof}:21:00:03`
                *${command.weather.hourlyforecast}:*
                `21 ☂\uFE0F +1° 71% 28${command.weather.meterspersecond} `
                `21 ☂\uFE0F +2° 72% 29${command.weather.meterspersecond} `
                `21 ☔ +3° 73% 30${command.weather.meterspersecond} `
                `21 ❄ +4° 74% 31${command.weather.meterspersecond} `
                `21 ❄ +5° 75% 32${command.weather.meterspersecond} `
                *${command.weather.dailyforecast}:*
                `02 Sun. ☂\uFE0F +2° ☂\uFE0F +2°`
                `03 Mon. ☔ +3° ☔ +3°`
                `04 Tue. ❄ +4° ❄ +4°`
                `05 Wed. ❄ +5° ❄ +5°`
                """;
        final String token = "token";
        final int cityId = 123;
        final String lang = "en";
        final String expectedCurrentWeatherApiUrl = "https://api.openweathermap.org/data/2.5/weather?lang=" + lang + "&units=metric&appid=" + token + "&id=" + cityId;
        final String expectedForecastWeatherApiUrl = "https://api.openweathermap.org/data/2.5/forecast?lang=" + lang + "&units=metric&appid=" + token + "&id=" + cityId;
        BotRequest request = TestUtils.getRequestFromGroup("weather " + cityId);
        Message message = request.getMessage();

        when(propertiesConfig.getOpenweathermapId()).thenReturn(token);
        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(lang);
        when(weatherCurrentResponseEntity.getBody()).thenReturn(getSomeCurrentWeather());
        when(botRestTemplate.getForEntity(expectedCurrentWeatherApiUrl, Weather.WeatherCurrent.class)).thenReturn(weatherCurrentResponseEntity);
        when(weatherForecastResponseEntity.getBody()).thenReturn(getSomeForecastWeather());
        when(botRestTemplate.getForEntity(expectedForecastWeatherApiUrl, Weather.WeatherForecast.class)).thenReturn(weatherForecastResponseEntity);
        when(internationalizationService.internationalize(anyString(), anyString())).thenAnswer(answer -> answer.getArgument(0));

        BotResponse botResponse = weather.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    private Weather.WeatherCurrent getSomeCurrentWeather() {
        return new Weather.WeatherCurrent()
                .setBase("base")
                .setDt(946674000)
                .setVisibility(1000.123D)
                .setTimezone(3)
                .setId(123L)
                .setName("cityName")
                .setCod(1234)
                .setWeather(List.of(
                        getSomeWeatherData(1)))
                .setClouds(new Weather.Clouds().setAll(1001))
                .setWind(new Weather.Wind().setDeg(25).setGust(26.123D).setSpeed(27.123D))
                .setRain(new Weather.HourlyForecast().setOneHours(28.123D).setThreeHours(29.123D).setSixHours(30.123D))
                .setSnow(new Weather.HourlyForecast().setOneHours(31.123D).setThreeHours(32.123D).setSixHours(33.123D))
                .setSys(new Weather.Sys().setType(1002).setId(1003).setMessage(34.123D).setCountry("ru").setSunrise(1767247980).setSunset(1767272820))
                .setMain(new Weather.Main()
                        .setTemp(0.123D)
                        .setFeelsLike(1.123D)
                        .setPressure(743.123D)
                        .setHumidity(70.123D)
                        .setTempMin(-1.123D)
                        .setTempMax(1.123)
                        .setSeaLevel(146.123D)
                        .setGrndLevel(0D)
                        .setTempKf(10));
    }

    private Weather.WeatherForecast getSomeForecastWeather() {
        return new Weather.WeatherForecast()
                .setCod("cod")
                .setMessage(1004)
                .setCnt(1005)
                .setCity(new Weather.City()
                        .setId(1006)
                        .setName("cityName")
                        .setCountry("ru")
                        .setPopulation(300000)
                        .setTimezone(3)
                        .setSunrise(1767247980)
                        .setSunset(1767272820))
                .setList(Stream.of(1, 2, 3, 4, 5).map(this::getSomeForecastData).toList());
    }

    private Weather.WeatherForecastData getSomeForecastData(int index) {
        return new Weather.WeatherForecastData()
                .setDt(946674000 + (index * 24 * 60 * 60))
                .setVisibility(1007 + index)
                .setPop(1008 + index)
                .setDtTxt("dt_txt" + index)
                .setMain(new Weather.Main()
                        .setTemp(0.123D + index)
                        .setFeelsLike(1.123D + index)
                        .setPressure(743.123D + index)
                        .setHumidity(70.123D + index)
                        .setTempMin(-1.123D + index)
                        .setTempMax(1.123 + index)
                        .setSeaLevel(146.123D + index)
                        .setGrndLevel(0D + index)
                        .setTempKf(10 + index))
                .setWeather(List.of(
                        getSomeWeatherData(index)))
                .setClouds(new Weather.Clouds().setAll(1001 + index))
                .setWind(new Weather.Wind().setDeg(25 + index).setGust(26.123D + index).setSpeed(27.123D + index))
                .setRain(new Weather.HourlyForecast().setOneHours(28.123D + index).setThreeHours(29.123D + index).setSixHours(30.123D + index))
                .setSnow(new Weather.HourlyForecast().setOneHours(31.123D + index).setThreeHours(32.123D + index).setSixHours(33.123D + index))
                .setSys(new Weather.Sys().setType(1002 + index).setId(1003 + index).setMessage(34.123D + index).setCountry("ru").setSunrise(1767247980 + index).setSunset(1767272820 + index));
    }

    private Weather.WeatherData getSomeWeatherData(int index) {
        return new Weather.WeatherData()
                .setId(200 + (index * 100))
                .setMain("main")
                .setDescription("desc")
                .setIcon("icon");
    }



}