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
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.services.UserCityService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewYearTest {

    private static final String DEFAULT_TIME_ZONE = "GMT+03:00";
    private static final LocalDateTime CURRENT_DATE_TIME = LocalDateTime.of(2007, 2, 3, 4, 5, 6);

    @Mock
    private Bot bot;
    @Mock
    private UserCityService userCityService;
    @Mock
    private Clock clock;

    @InjectMocks
    private NewYear newYear;

    @Test
    void parseWithArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("newyear test");
        List<BotResponse> botResponses = newYear.parse(request);
        assertTrue(botResponses.isEmpty());
        verify(bot, never()).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithoutArgumentsUnknownUserTest() {
        final String expectedResponseText = "${command.newyear.caption}: *331 ${utils.date.d}. 19 ${utils.date.h}. 54 ${utils.date.m}. 54 ${utils.date.s}. * (GMT+03:00)";
        BotRequest request = TestUtils.getRequestFromGroup("newyear");

        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.of(DEFAULT_TIME_ZONE)).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.of(DEFAULT_TIME_ZONE));

        BotResponse botResponse = newYear.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithoutArgumentsKnownUserTest() {
        final String expectedResponseText = "${command.newyear.caption}: *331 ${utils.date.d}. 19 ${utils.date.h}. 54 ${utils.date.m}. 54 ${utils.date.s}. * (GMT+04:00)";
        BotRequest request = TestUtils.getRequestFromGroup("newyear");

        when(userCityService.get(request.getMessage().getUser(), request.getMessage().getChat())).thenReturn(new UserCity().setCity(new City().setTimeZone("GMT+04:00")));
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.of(DEFAULT_TIME_ZONE)).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.of(DEFAULT_TIME_ZONE));

        BotResponse botResponse = newYear.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

}