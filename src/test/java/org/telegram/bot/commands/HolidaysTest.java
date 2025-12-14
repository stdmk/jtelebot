package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Holiday;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.HolidayService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidaysTest {

    private static final LocalDate CURRENT_DATE = LocalDate.of(2007, 1, 2);

    @Mock
    private Bot bot;
    @Mock
    private HolidayService holidayService;
    @Mock
    private SpeechService speechService;
    @Mock
    private LanguageResolver languageResolver;
    @Mock
    private Clock clock;

    @InjectMocks
    private Holidays holidays;

    @Test
    void parseComingHolidaysTest() {
        final String expectedResponseText = """
                <u>${command.holidays.caption}:</u>
                <b>Tue. 02.01 </b><i>holiday1</i> (1 ${command.holidays.years1})
                /holidays_1
                <b>Tue. 02.01 </b><i>holiday2</i>\s
                /holidays_2
                """;
        BotRequest request = TestUtils.getRequestFromGroup("holidays");
        List<Holiday> holidayList = getSomeHolidays();

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(holidayService.get(any(Chat.class))).thenReturn(holidayList);
        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");

        BotResponse botResponse = holidays.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(textResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseHolidayInfoWithCorruptedIdTest() {
        BotRequest request = TestUtils.getRequestFromGroup("holidays_a");
        assertThrows(BotException.class, () -> holidays.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseHolidayInfoWithUnknownHolidayIdTest() {
        BotRequest request = TestUtils.getRequestFromGroup("holidays_1");
        assertThrows(BotException.class, () -> holidays.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseHolidayInfoTest() {
        final String expectedResponseText = """
                <u>holiday1</u>
                <i>02.01.2007 Tue.</i>\s
                ${command.holidays.author}: <a href="tg://user?id=1">username</a>""";
        BotRequest request = TestUtils.getRequestFromGroup("holidays_1");

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(holidayService.get(anyLong()))
                .thenReturn(new Holiday()
                        .setId(1L)
                        .setDate(CURRENT_DATE.minusYears(1))
                        .setName("holiday1")
                        .setUser(TestUtils.getUser()));
        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");

        BotResponse botResponse = holidays.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(textResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithSearchHolidaysByTextTest() {
        final String expectedResponseText = """
                <u>${command.holidays.searchresults}:</u>
                <b>Tue. 02.01 </b><i>holiday1</i> (1 ${command.holidays.years1})
                /holidays_1
                <b>Tue. 02.01 </b><i>holiday2</i>\s
                /holidays_2
                """;
        BotRequest request = TestUtils.getRequestFromGroup("holidays test");
        List<Holiday> holidayList = getSomeHolidays();

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(holidayService.get(any(Chat.class), anyString())).thenReturn(holidayList);
        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");

        BotResponse botResponse = holidays.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(textResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithSearchHolidaysByDateWithCorruptedDateTest() {
        BotRequest request = TestUtils.getRequestFromGroup("holidays aa.bb");

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        assertThrows(BotException.class, () -> holidays.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWithSearchHolidaysByDateWithoutHolidaysTest() {
        final String expectedResponseText = "${command.holidays.noholidays}";
        BotRequest request = TestUtils.getRequestFromGroup("holidays 11.12");
        List<Holiday> holidayList = getSomeHolidays();

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(holidayService.get(any(Chat.class))).thenReturn(holidayList);

        BotResponse botResponse = holidays.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(textResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithSearchHolidaysByDateTest() {
        final String expectedResponseText = """
                <u>02.01.2007</u> (Tue.)
                <b>02.01 </b><i>holiday1</i> (1 ${command.holidays.years1})
                /holidays_1
                <b>02.01 </b><i>holiday2</i>\s
                /holidays_2
                """;
        BotRequest request = TestUtils.getRequestFromGroup("holidays 02.01");
        List<Holiday> holidayList = getSomeHolidays();

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(holidayService.get(any(Chat.class))).thenReturn(holidayList);
        when(languageResolver.getChatLanguageCode(request)).thenReturn("en");

        BotResponse botResponse = holidays.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(textResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    private List<Holiday> getSomeHolidays() {
        return List.of(
                new Holiday().setId(1L).setDate(CURRENT_DATE.minusYears(1)).setName("holiday1").setHasYear(true),
                new Holiday().setId(2L).setDate(CURRENT_DATE.minusYears(2)).setName("holiday2").setHasYear(false)
        );
    }

}