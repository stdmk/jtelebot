package org.telegram.bot.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.providers.daysoff.DaysOffProvider;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.services.LanguageResolver;

import java.time.*;
import java.util.*;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.telegram.bot.TestUtils.*;

@ExtendWith(MockitoExtension.class)
class CalendarTest {

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    @Mock
    private Bot bot;
    @Mock
    private UserCityService userCityService;
    @Mock
    private SpeechService speechService;
    @Mock
    private LanguageResolver languageResolver;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private DaysOffProvider daysOffProvider = Mockito.mock(DaysOffProvider.class);
    @Mock
    private Clock clock;
    @Mock
    private ResponseEntity<Object> responseEntity;

    @InjectMocks
    private Calendar calendar;

    @BeforeEach
    public void init() {
        ReflectionTestUtils.setField(calendar, "daysOffProviderList", List.of(daysOffProvider));
    }

    @Test
    void printCurrentCalendarTest() {
        final String expectedResponseText = "<b>January 2007</b>\n" +
                "<code>${command.calendar.daysofweekstring}\n" +
                " 1   2*  3*  4*  5*  6   7  \n" +
                " 8   9  10  11  12  13  14  \n" +
                "15  16  17  18  19  20  21  \n" +
                "22  23  24  25  26  27  28  \n" +
                "29  30  31  \n" +
                "</code>\n" +
                "<b>${command.calendar.holidayscaption}: </b>\n" +
                "<b>01.01.2007</b> — test.";
        LocalDate date = LocalDate.of(2007, 1, 1);
        Calendar.PublicHoliday publicHoliday = new Calendar.PublicHoliday();
        publicHoliday.setDate(date);
        publicHoliday.setLocalName("test");
        Calendar.PublicHoliday[] publicHolidays = List.of(publicHoliday).toArray(Calendar.PublicHoliday[]::new);
        BotRequest request = getRequestFromGroup();

        when(responseEntity.getBody()).thenReturn(publicHolidays);
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(responseEntity);
        when(daysOffProvider.getLocale()).thenReturn(DEFAULT_LOCALE);
        when(daysOffProvider.getDaysOffInMonth(anyInt(), anyInt())).thenReturn(List.of(2, 3, 4, 5));
        when(clock.instant()).thenReturn(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(languageResolver.getChatLanguageCode(any(Message.class), any(User.class))).thenReturn("en");

        BotResponse response = calendar.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());
        TextResponse textResponse = checkDefaultTextResponseParams(response, FormattingStyle.HTML);

        String actualResponseText = textResponse.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void printCalendarForDateTest() {
        final String expectedResponseText = "<b>January 2007</b>\n" +
                "<code>${command.calendar.daysofweekstring}\n" +
                " 1   2*  3*  4*  5*  6   7  \n" +
                " 8   9  10  11  12  13  14  \n" +
                "15  16  17  18  19  20  21  \n" +
                "22  23  24  25  26  27  28  \n" +
                "29  30  31  \n" +
                "</code>\n" +
                "<b>${command.calendar.holidayscaption}: </b>\n";
        LocalDate date = LocalDate.of(2007, 5, 1);
        Calendar.PublicHoliday publicHoliday = new Calendar.PublicHoliday();
        publicHoliday.setDate(date);
        publicHoliday.setLocalName("test");
        Calendar.PublicHoliday[] publicHolidays = List.of(publicHoliday).toArray(Calendar.PublicHoliday[]::new);
        BotRequest request = getRequestFromGroup("calendar 01.2007");

        when(responseEntity.getBody()).thenReturn(publicHolidays);
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(responseEntity);
        when(daysOffProvider.getLocale()).thenReturn(DEFAULT_LOCALE);
        when(daysOffProvider.getDaysOffInMonth(anyInt(), anyInt())).thenReturn(List.of(2, 3, 4, 5));
        when(clock.instant()).thenReturn(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(languageResolver.getChatLanguageCode(any(Message.class), any(User.class))).thenReturn("en");

        BotResponse response = calendar.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(userCityService).getZoneIdOfUser(any(Chat.class), any(User.class));
        TextResponse textResponse = checkDefaultTextResponseParams(response, FormattingStyle.HTML, false, true);

        String actualResponseText = textResponse.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void calendarWithRestClientExceptionTest() {
        final String expectedResponseText = "<b>January 2007</b>\n" +
                "<code>${command.calendar.daysofweekstring}\n" +
                " 1   2*  3*  4*  5*  6   7  \n" +
                " 8   9  10  11  12  13  14  \n" +
                "15  16  17  18  19  20  21  \n" +
                "22  23  24  25  26  27  28  \n" +
                "29  30  31  \n" +
                "</code>\n" +
                "<b>${command.calendar.holidayscaption}: </b>\n";
        LocalDate date = LocalDate.of(2007, 5, 1);
        BotRequest request = getRequestFromGroup("calendar 01.2007");

        when(botRestTemplate.getForEntity(anyString(), any())).thenThrow(new RestClientException("test"));
        when(daysOffProvider.getLocale()).thenReturn(DEFAULT_LOCALE);
        when(daysOffProvider.getDaysOffInMonth(anyInt(), anyInt())).thenReturn(List.of(2, 3, 4, 5));
        when(clock.instant()).thenReturn(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(languageResolver.getChatLanguageCode(any(Message.class), any(User.class))).thenReturn("en");

        BotResponse response = calendar.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(userCityService).getZoneIdOfUser(any(Chat.class), any(User.class));
        TextResponse textResponse = checkDefaultTextResponseParams(response, FormattingStyle.HTML, false, true);

        String actualResponseText = textResponse.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void calendarWithEmptyResponseTest() {
        final String expectedResponseText = "<b>January 2007</b>\n" +
                "<code>${command.calendar.daysofweekstring}\n" +
                " 1   2   3   4   5   6   7  \n" +
                " 8   9  10  11  12  13  14  \n" +
                "15  16  17  18  19  20  21  \n" +
                "22  23  24  25  26  27  28  \n" +
                "29  30  31  \n" +
                "</code>\n" +
                "<b>${command.calendar.holidayscaption}: </b>\n";
        LocalDate date = LocalDate.of(2007, 5, 1);
        BotRequest request = getRequestFromGroup("calendar january 2007");

        Map<Integer, Set<String>> monthValueNamesMap = new HashMap<>();
        monthValueNamesMap.put(1, Set.of("january"));
        ReflectionTestUtils.setField(calendar, "monthValueNamesMap", monthValueNamesMap);

        when(clock.instant()).thenReturn(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(responseEntity);
        when(daysOffProvider.getLocale()).thenReturn(DEFAULT_LOCALE);
        when(daysOffProvider.getDaysOffInMonth(anyInt(), anyInt())).thenReturn(List.of());
        when(responseEntity.getBody()).thenReturn(null);
        when(languageResolver.getChatLanguageCode(any(Message.class), any(User.class))).thenReturn("en");

        BotResponse response = calendar.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(userCityService).getZoneIdOfUser(any(Chat.class), any(User.class));
        TextResponse textResponse = checkDefaultTextResponseParams(response, FormattingStyle.HTML, false, true);

        String actualResponseText = textResponse.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void calendarWithWrongDateArgumentParsingTest() {
        final String expectedErrorMessage = "error";
        BotRequest request = getRequestFromGroup("calendar 32.3033");
        when(speechService.getRandomMessageByTag(any(BotSpeechTag.class))).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> calendar.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        assertEquals(expectedErrorMessage, botException.getMessage());
    }

    @Test
    void calendarWithWrongMonth1ArgumentParsingTest() {
        final String expectedErrorMessage = "error";
        BotRequest request = getRequestFromGroup("calendar 2022");
        LocalDate date = LocalDate.of(2007, 5, 1);

        when(clock.instant()).thenReturn(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(speechService.getRandomMessageByTag(any(BotSpeechTag.class))).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> calendar.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        assertEquals(expectedErrorMessage, botException.getMessage());
    }

    @Test
    void calendarWithWrongMonthArgumentParsingTest() {
        final String expectedErrorMessage = "error";
        BotRequest request = getRequestFromGroup("calendar jnary 2007");

        when(speechService.getRandomMessageByTag(any(BotSpeechTag.class))).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> calendar.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        assertEquals(expectedErrorMessage, botException.getMessage());
    }

    @Test
    void calendarWithUnexpectedArgumentParsingTest() {
        final String expectedErrorMessage = "error";
        BotRequest request = getRequestFromGroup("calendar abv");
        LocalDate date = LocalDate.of(2007, 5, 1);

        when(clock.instant()).thenReturn(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        when(speechService.getRandomMessageByTag(any(BotSpeechTag.class))).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> calendar.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        assertEquals(expectedErrorMessage, botException.getMessage());
    }

    @Test
    void calendarWithExpectedMonthNameArgumentTest() {
        BotRequest request = getRequestFromGroup("календарь january");
        LocalDate date = LocalDate.of(2007, 5, 1);

        Map<Integer, Set<String>> monthValueNamesMap = new HashMap<>();
        monthValueNamesMap.put(1, Set.of("january"));
        ReflectionTestUtils.setField(calendar, "monthValueNamesMap", monthValueNamesMap);

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(null);
        when(clock.instant()).thenReturn(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        BotResponse response = calendar.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(userCityService).getZoneIdOfUser(any(Chat.class), any(User.class));
        checkDefaultTextResponseParams(response, FormattingStyle.HTML, false, true);
    }

    @Test
    void calendarWithExpectedMonthNumberArgumentTest() {
        BotRequest request = getRequestWithCallback("calendar 1");
        LocalDate date = LocalDate.of(2007, 5, 1);

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(null);
        when(clock.instant()).thenReturn(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        BotResponse response = calendar.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(userCityService).getZoneIdOfUser(any(Chat.class), any(User.class));
        checkDefaultEditResponseParams(response, FormattingStyle.HTML, false, true);
    }
}