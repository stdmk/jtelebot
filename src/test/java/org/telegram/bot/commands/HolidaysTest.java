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
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.HolidayService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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
        final String expectedResponseText = "<u>${command.holidays.caption}:</u>\n" +
                "<b>Tue. 02.01 </b><i>holiday1</i> (1 ${command.holidays.years1})\n" +
                "/holidays_1\n" +
                "<b>Tue. 02.01 </b><i>holiday2</i> (2 ${command.holidays.yearsparentcase})\n" +
                "/holidays_2\n";
        Update update = TestUtils.getUpdateFromGroup("holidays");
        List<Holiday> holidayList = getSomeHolidays();

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(holidayService.get(any(Chat.class))).thenReturn(holidayList);
        when(languageResolver.getChatLanguageCode(update)).thenReturn("en");

        SendMessage sendMessage = holidays.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage);

        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void parseHolidayInfoWithCorruptedIdTest() {
        Update update = TestUtils.getUpdateFromGroup("holidays_a");
        assertThrows(BotException.class, () -> holidays.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseHolidayInfoWithUnknownHolidayIdTest() {
        Update update = TestUtils.getUpdateFromGroup("holidays_1");
        assertThrows(BotException.class, () -> holidays.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseHolidayInfoTest() {
        final String expectedResponseText = "<u>holiday1</u>\n" +
                "<i>02.01.2006 Tue.</i> (1 ${command.holidays.years1})\n" +
                "${command.holidays.author}: <a href=\"tg://user?id=1\">username</a>";
        Update update = TestUtils.getUpdateFromGroup("holidays_1");

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(holidayService.get(anyLong()))
                .thenReturn(new Holiday()
                        .setId(1L)
                        .setDate(CURRENT_DATE.minusYears(1))
                        .setName("holiday1")
                        .setUser(TestUtils.getUser()));
        when(languageResolver.getChatLanguageCode(update)).thenReturn("en");

        SendMessage sendMessage = holidays.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage);

        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void parseWithSearchHolidaysByTextTest() {
        final String expectedResponseText = "<u>${command.holidays.searchresults}:</u>\n" +
                "<b>Tue. 02.01 </b><i>holiday1</i> (1 ${command.holidays.years1})\n" +
                "/holidays_1\n" +
                "<b>Tue. 02.01 </b><i>holiday2</i> (2 ${command.holidays.yearsparentcase})\n" +
                "/holidays_2\n";
        Update update = TestUtils.getUpdateFromGroup("holidays test");
        List<Holiday> holidayList = getSomeHolidays();

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(holidayService.get(any(Chat.class), anyString())).thenReturn(holidayList);
        when(languageResolver.getChatLanguageCode(update)).thenReturn("en");

        SendMessage sendMessage = holidays.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage);

        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void parseWithSearchHolidaysByDateWithCorruptedDateTest() {
        Update update = TestUtils.getUpdateFromGroup("holidays aa.bb");

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        assertThrows(BotException.class, () -> holidays.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWithSearchHolidaysByDateWithoutHolidaysTest() {
        final String expectedResponseText = "${command.holidays.noholidays}";
        Update update = TestUtils.getUpdateFromGroup("holidays 11.12");
        List<Holiday> holidayList = getSomeHolidays();

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(holidayService.get(any(Chat.class))).thenReturn(holidayList);

        SendMessage sendMessage = holidays.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage);

        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void parseWithSearchHolidaysByDateTest() {
        final String expectedResponseText = "<u>02.01.2007</u> (Tue.)\n" +
                "<b>02.01 </b><i>holiday1</i> (1 ${command.holidays.years1})\n" +
                "/holidays_1\n" +
                "<b>02.01 </b><i>holiday2</i> (2 ${command.holidays.yearsparentcase})\n" +
                "/holidays_2\n";
        Update update = TestUtils.getUpdateFromGroup("holidays 02.01");
        List<Holiday> holidayList = getSomeHolidays();

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(holidayService.get(any(Chat.class))).thenReturn(holidayList);
        when(languageResolver.getChatLanguageCode(update)).thenReturn("en");

        SendMessage sendMessage = holidays.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage);

        assertEquals(expectedResponseText, sendMessage.getText());
    }

    private List<Holiday> getSomeHolidays() {
        return List.of(new Holiday().setId(1L).setDate(CURRENT_DATE.minusYears(1)).setName("holiday1"),
                new Holiday().setId(2L).setDate(CURRENT_DATE.minusYears(2)).setName("holiday2"));
    }

}