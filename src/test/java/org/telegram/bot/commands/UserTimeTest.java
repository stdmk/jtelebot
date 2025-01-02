package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.City;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CityService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.services.UserService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTimeTest {

    private static final LocalDateTime DATE_TIME_NOW = LocalDateTime.of(2000, 1, 1, 0, 0);

    @Mock
    private Bot bot;
    @Mock
    private UserService userService;
    @Mock
    private UserCityService userCityService;
    @Mock
    private CityService cityService;
    @Mock
    private SpeechService speechService;
    @Mock
    private Clock clock;

    @InjectMocks
    private UserTime userTime;

    @Test
    void parseWithoutArgumentsAndRepliedMessageTest() {
        final String expectedResponseText = "[username](tg://user?id=1) ${command.usertime.citynotset}";
        BotRequest request = TestUtils.getRequestFromGroup("time");
        Message message = request.getMessage();

        when(userService.get(message.getUser().getUserId())).thenReturn(message.getUser());

        BotResponse botResponse = userTime.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithCityAsArgumentWithoutRepliedMessageTest() {
        final String cityNameEn = "kitezhgrad";
        final String cityNameRu = "Китежград";
        final String expectedResponseText = "${command.usertime.incity} Китежград ${command.usertime.now}: *00:00:00*";
        BotRequest request = TestUtils.getRequestFromGroup("time " + cityNameEn);

        when(clock.instant()).thenReturn(DATE_TIME_NOW.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        City city = new City();
        city.setNameRu(cityNameRu);
        city.setTimeZone(ZoneId.systemDefault().toString());
        when(cityService.get(cityNameEn)).thenReturn(city);

        BotResponse botResponse = userTime.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseUnknownAsArgumentWithoutRepliedMessageTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("time city");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> userTime.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithUserAsArgumentWithoutRepliedMessageTest() {
        final String cityNameRu = "Китежград";
        final String username = "username";
        final String expectedResponseText = "${command.usertime.at} [username](tg://user?id=1) ${command.usertime.now} *00:00:00*";
        BotRequest request = TestUtils.getRequestFromGroup("time " + username);
        Message message = request.getMessage();

        when(userService.get(username)).thenReturn(message.getUser());
        when(clock.instant()).thenReturn(DATE_TIME_NOW.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        City city = new City();
        city.setNameRu(cityNameRu);
        city.setTimeZone(ZoneId.systemDefault().toString());
        when(userCityService.get(message.getUser(), message.getChat())).thenReturn(new UserCity().setCity(city));

        BotResponse botResponse = userTime.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithoutArgumentsWithRepliedMessageTest() {
        final String cityNameRu = "Китежград";
        final String expectedResponseText = "${command.usertime.at} [username](tg://user?id=2) ${command.usertime.now} *00:00:00*\n${command.usertime.was}: *31.12.1999 22:00:00*";
        BotRequest request = TestUtils.getRequestWithRepliedMessage("");
        Message message = request.getMessage();
        Message repliedToMessage = message.getReplyToMessage();
        repliedToMessage.setDateTime(DATE_TIME_NOW.minusHours(2));

        when(userService.get(repliedToMessage.getUser().getUserId())).thenReturn(repliedToMessage.getUser());
        when(clock.instant()).thenReturn(DATE_TIME_NOW.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        City city = new City();
        city.setNameRu(cityNameRu);
        city.setTimeZone(ZoneId.systemDefault().toString());
        when(userCityService.get(repliedToMessage.getUser(), message.getChat())).thenReturn(new UserCity().setCity(city));

        BotResponse botResponse = userTime.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(message.getChatId());
    }

}