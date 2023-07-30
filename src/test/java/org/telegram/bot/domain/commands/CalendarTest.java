package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserCityService;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.telegram.bot.TestUtils.*;

@ExtendWith(MockitoExtension.class)
class CalendarTest {

    @Mock
    private Bot bot;
    @Mock
    private UserCityService userCityService;
    @Mock
    private SpeechService speechService;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private Clock clock;
    @Mock
    private ResponseEntity<Object> responseEntity;

    @InjectMocks
    private Calendar calendar;

    @Test
    void printCurrentCalendarTest() {
        final String expectedResponseText = "<b>январь 2007</b>\n" +
                "<code>ПН  ВТ  СР  ЧТ  ПТ  СБ  ВС\n" +
                " 1*  2   3   4   5   6   7  \n" +
                " 8   9  10  11  12  13  14  \n" +
                "15  16  17  18  19  20  21  \n" +
                "22  23  24  25  26  27  28  \n" +
                "29  30  31  \n" +
                "</code>\n" +
                "<b>Праздники: </b>\n" +
                "<b>01.01.2007</b> — test.";
        LocalDate date = LocalDate.of(2007, 1, 1);
        Calendar.PublicHoliday publicHoliday = new Calendar.PublicHoliday();
        publicHoliday.setDate(date);
        publicHoliday.setLocalName("test");
        Calendar.PublicHoliday[] publicHolidays = List.of(publicHoliday).toArray(Calendar.PublicHoliday[]::new);
        Update update = getUpdateFromGroup();

        when(responseEntity.getBody()).thenReturn(publicHolidays);
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(responseEntity);
        when(clock.instant()).thenReturn(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        calendar.parse(update);
        PartialBotApiMethod<?> method = calendar.parse(update);
        SendMessage sendMessage = checkDefaultSendMessageParams(method, ParseMode.HTML);

        String actualResponseText = sendMessage.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void printCalendarForDateTest() {
        final String expectedResponseText = "<b>январь 2007</b>\n" +
                "<code>ПН  ВТ  СР  ЧТ  ПТ  СБ  ВС\n" +
                " 1   2   3   4   5   6   7  \n" +
                " 8   9  10  11  12  13  14  \n" +
                "15  16  17  18  19  20  21  \n" +
                "22  23  24  25  26  27  28  \n" +
                "29  30  31  \n" +
                "</code>\n" +
                "<b>Праздники: </b>\n";
        LocalDate date = LocalDate.of(2007, 5, 1);
        Calendar.PublicHoliday publicHoliday = new Calendar.PublicHoliday();
        publicHoliday.setDate(date);
        publicHoliday.setLocalName("test");
        Calendar.PublicHoliday[] publicHolidays = List.of(publicHoliday).toArray(Calendar.PublicHoliday[]::new);
        Update update = getUpdateFromGroup("calendar 01.2007");

        when(responseEntity.getBody()).thenReturn(publicHolidays);
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(responseEntity);
        when(clock.instant()).thenReturn(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        PartialBotApiMethod<?> method = calendar.parse(update);
        SendMessage sendMessage = checkDefaultSendMessageParams(method, ParseMode.HTML, false, true);

        String actualResponseText = sendMessage.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void calendarWithRestClientExceptionTest() {
        final String expectedResponseText = "<b>январь 2007</b>\n" +
                "<code>ПН  ВТ  СР  ЧТ  ПТ  СБ  ВС\n" +
                " 1   2   3   4   5   6   7  \n" +
                " 8   9  10  11  12  13  14  \n" +
                "15  16  17  18  19  20  21  \n" +
                "22  23  24  25  26  27  28  \n" +
                "29  30  31  \n" +
                "</code>\n" +
                "<b>Праздники: </b>\n";
        LocalDate date = LocalDate.of(2007, 5, 1);
        Update update = getUpdateFromGroup("calendar 01.2007");

        when(botRestTemplate.getForEntity(anyString(), any())).thenThrow(new RestClientException("test"));
        when(clock.instant()).thenReturn(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        PartialBotApiMethod<?> method = calendar.parse(update);
        SendMessage sendMessage = checkDefaultSendMessageParams(method, ParseMode.HTML, false, true);

        String actualResponseText = sendMessage.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void calendarWithEmptyResponseTest() {
        final String expectedResponseText = "<b>январь 2007</b>\n" +
                "<code>ПН  ВТ  СР  ЧТ  ПТ  СБ  ВС\n" +
                " 1   2   3   4   5   6   7  \n" +
                " 8   9  10  11  12  13  14  \n" +
                "15  16  17  18  19  20  21  \n" +
                "22  23  24  25  26  27  28  \n" +
                "29  30  31  \n" +
                "</code>\n" +
                "<b>Праздники: </b>\n";
        LocalDate date = LocalDate.of(2007, 5, 1);
        Update update = getUpdateFromGroup("календарь январь 2007");

        when(clock.instant()).thenReturn(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(null);

        PartialBotApiMethod<?> method = calendar.parse(update);
        SendMessage sendMessage = checkDefaultSendMessageParams(method, ParseMode.HTML, false, true);

        String actualResponseText = sendMessage.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void calendarWithWrongDateArgumentParsingTest() {
        final String expectedErrorMessage = "error";
        Update update = getUpdateFromGroup("calendar 32.3033");

        when(speechService.getRandomMessageByTag(any(BotSpeechTag.class))).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> calendar.parse(update));
        assertEquals(expectedErrorMessage, botException.getMessage());
    }

    @Test
    void calendarWithWrongMonthArgumentParsingTest() {
        final String expectedErrorMessage = "error";
        Update update = getUpdateFromGroup("calendar инварь 2007");

        when(speechService.getRandomMessageByTag(any(BotSpeechTag.class))).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> calendar.parse(update));
        assertEquals(expectedErrorMessage, botException.getMessage());
    }

    @Test
    void calendarWithUnexpectedArgumentParsingTest() {
        final String expectedErrorMessage = "error";
        Update update = getUpdateFromGroup("calendar абв");
        LocalDate date = LocalDate.of(2007, 5, 1);

        when(clock.instant()).thenReturn(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        when(speechService.getRandomMessageByTag(any(BotSpeechTag.class))).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> calendar.parse(update));
        assertEquals(expectedErrorMessage, botException.getMessage());
    }

    @Test
    void calendarWithExpectedMonthNameArgumentTest() {
        Update update = getUpdateFromGroup("календарь январь");
        LocalDate date = LocalDate.of(2007, 5, 1);

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(null);
        when(clock.instant()).thenReturn(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        PartialBotApiMethod<?> method = calendar.parse(update);
        checkDefaultSendMessageParams(method, ParseMode.HTML, false, true);
    }

    @Test
    void calendarWithExpectedMonthNumberArgumentTest() {
        Update update = getUpdateWithCallback("calendar 1");
        LocalDate date = LocalDate.of(2007, 5, 1);

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(null);
        when(clock.instant()).thenReturn(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        PartialBotApiMethod<?> method = calendar.parse(update);
        checkDefaultEditMessageTextParams(method, ParseMode.HTML, false, true);
    }
}