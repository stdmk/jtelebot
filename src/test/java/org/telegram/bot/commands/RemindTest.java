package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.Reminder;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.DateUtils;

import java.time.*;
import java.util.Set;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemindTest {

    private static final LocalDate CURRENT_DATE = LocalDate.of(2000, 1, 1);
    private static final LocalTime CURRENT_TIME = LocalTime.of(1, 2, 3);
    private static final LocalDateTime CURRENT_DATE_TIME = LocalDateTime.of(CURRENT_DATE, CURRENT_TIME);
    private static final String USER_LANG_CODE = "en";

    @Mock
    private Bot bot;
    @Mock
    private ReminderService reminderService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private UserCityService userCityService;
    @Mock
    private SpeechService speechService;
    @Mock
    private InternationalizationService internationalizationService;
    @Mock
    private LanguageResolver languageResolver;
    @Mock
    private BotStats botStats;
    @Mock
    private Clock clock;

    @InjectMocks
    private Remind remind;

    @Test
    void postConstructTest() {
        final String expectedAfterMinutesPattern1 = "(in|через)\\s+(\\d+)\\s+((\\bminute\\b)|(\\bminutes\\b)|(\\bминуту\\b)|(\\bминуты\\b)|(\\bминут\\b))";
        final String expectedAfterMinutesPattern2 = "(через|in)\\s+(\\d+)\\s+((\\bминуту\\b)|(\\bминуты\\b)|(\\bминут\\b)|(\\bminute\\b)|(\\bminutes\\b))";
        final String expectedAfterHoursPattern1 = "(in|через)\\s+(\\d+)\\s+((\\bhour\\b)|(\\bhours\\b)|(\\bчас\\b)|(\\bчаса\\b)|(\\bчасов\\b))";
        final String expectedAfterHoursPattern2 = "(через|in)\\s+(\\d+)\\s+((\\bчас\\b)|(\\bчаса\\b)|(\\bчасов\\b)|(\\bhour\\b)|(\\bhours\\b))";
        final String expectedAfterDaysPattern1 = "(in|через)\\s+(\\d+)\\s+((\\bday\\b)|(\\bdays\\b)|(\\bдень\\b)|(\\bдня\\b)|(\\bдней\\b))";
        final String expectedAfterDaysPattern2 = "(через|in)\\s+(\\d+)\\s+((\\bдень\\b)|(\\bдня\\b)|(\\bдней\\b)|(\\bday\\b)|(\\bdays\\b))";

        when(internationalizationService.getAllTranslations("command.remind.in")).thenReturn(Set.of("in", "через"));
        when(internationalizationService.getAllTranslations("command.remind.minutes")).thenReturn(Set.of("minute#minutes", "минуту#минуты#минут"));
        when(internationalizationService.getAllTranslations("command.remind.hours")).thenReturn(Set.of("hour#hours", "час#часа#часов"));
        when(internationalizationService.getAllTranslations("command.remind.days")).thenReturn(Set.of("day#days", "день#дня#дней"));

        when(internationalizationService.getAllTranslations("command.remind.datekeyword.today")).thenReturn(Set.of("today", "сегодня"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.aftertomorrow")).thenReturn(Set.of("after tomorrow", "послезавтра"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.tomorrow")).thenReturn(Set.of("tomorrow", "завтра"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.monday")).thenReturn(Set.of("on Monday#on monday", "в понедельник"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.tuesday")).thenReturn(Set.of("on Tuesday#on tuesday", "во вторник"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.wednesday")).thenReturn(Set.of("on Wednesday#on wednessday", "в среду"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.thursday")).thenReturn(Set.of("on Thursday#on thursday", "в четверг"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.friday")).thenReturn(Set.of("on Friday#on friday", "в пятницу"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.saturday")).thenReturn(Set.of("on Saturday#on saturday", "в субботу"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.sunday")).thenReturn(Set.of("on Sunday#on sunday", "в воскресенье"));

        when(internationalizationService.getAllTranslations("command.remind.timekeyword.morning")).thenReturn(Set.of("morning", "утром"));
        when(internationalizationService.getAllTranslations("command.remind.timekeyword.lunch")).thenReturn(Set.of("lunch", "в обед#к обеду"));
        when(internationalizationService.getAllTranslations("command.remind.timekeyword.afternoon")).thenReturn(Set.of("afternoon", "днём#днем"));
        when(internationalizationService.getAllTranslations("command.remind.timekeyword.evening")).thenReturn(Set.of("evening", "поздним вечером#поздно вечером"));
        when(internationalizationService.getAllTranslations("command.remind.timekeyword.dinner")).thenReturn(Set.of("dinner", "вечером"));
        when(internationalizationService.getAllTranslations("command.remind.timekeyword.night")).thenReturn(Set.of("night", "ночью"));


        ReflectionTestUtils.invokeMethod(remind, "postConstruct");

        Pattern actualAfterMinutesPattern = (Pattern) ReflectionTestUtils.getField(remind, "afterMinutesPattern");
        Pattern actualAfterHoursPattern = (Pattern) ReflectionTestUtils.getField(remind, "afterHoursPattern");
        Pattern actualAfterDaysPattern = (Pattern) ReflectionTestUtils.getField(remind, "afterDaysPattern");

        assertNotNull(actualAfterMinutesPattern);
        assertNotNull(actualAfterHoursPattern);
        assertNotNull(actualAfterDaysPattern);

        assertTrue(expectedAfterMinutesPattern1.equals(actualAfterMinutesPattern.pattern()) || expectedAfterMinutesPattern2.equals(actualAfterMinutesPattern.pattern()));
        assertTrue(expectedAfterHoursPattern1.equals(actualAfterHoursPattern.pattern()) || expectedAfterHoursPattern2.equals(actualAfterHoursPattern.pattern()));
        assertTrue(expectedAfterDaysPattern1.equals(actualAfterDaysPattern.pattern()) || expectedAfterDaysPattern2.equals(actualAfterDaysPattern.pattern()));

        Map<Set<String>, Function<ZoneId, LocalDate>> dateKeywords = (Map<Set<String>, Function<ZoneId, LocalDate>>) ReflectionTestUtils.getField(remind, "dateKeywords");
        assertNotNull(dateKeywords);
        Set<String> strings = dateKeywords.keySet().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        assertEquals(27, strings.size());

        Map<Set<String>, Function<ZoneId, LocalDate>> timeKeywords = (Map<Set<String>, Function<ZoneId, LocalDate>>) ReflectionTestUtils.getField(remind, "timeKeywords");
        assertNotNull(timeKeywords);
        strings = dateKeywords.keySet().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        assertEquals(27, strings.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "upd"})
    void parseWithoutArgumentsReminderListEmptyTest(String argument) {
        final String expectedResponseText = "<a href=\"tg://user?id=1\">username</a><b> ${command.remind.yourreminders}:</b>\n";
        BotRequest request = TestUtils.getRequestWithCallback("remind " + argument);
        Message message = request.getMessage();

        when(reminderService.getByChatAndUser(message.getChat(), message.getUser(), 0)).thenReturn(new PageImpl<>(List.of()));

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        List<List<KeyboardButton>> keyboardButtonsList = editResponse.getKeyboard().getKeyboardButtonsList();
        assertEquals(2, keyboardButtonsList.size());

        List<KeyboardButton> addRow = keyboardButtonsList.get(0);
        assertEquals(1, addRow.size());
        KeyboardButton addButton = addRow.get(0);
        assertEquals("\uD83C\uDD95${command.remind.button.add}", addButton.getName());
        assertEquals("remind add", addButton.getCallback());

        List<KeyboardButton> reloadRow = keyboardButtonsList.get(1);
        assertEquals(1, reloadRow.size());
        KeyboardButton reloadButton = reloadRow.get(0);
        assertEquals("\uD83D\uDD04${command.remind.button.reload}", reloadButton.getName());
        assertEquals("remind upd", reloadButton.getCallback());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCallbackWithoutArgumentsTest() {
        final String expectedResponseText = """
            <a href="tg://user?id=1">username</a><b> ${command.remind.yourreminders}:</b>
            31.12 01:01 (Fri. ) — reminder1 🔕
            01.01 01:02 (Sat. ) — reminder2 with very very very long text 🔔
            01.01 01:01 (Sat. ) — reminder3 🔔📆""";
        BotRequest request = TestUtils.getRequestWithCallback("remind");
        Message message = request.getMessage();

        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(USER_LANG_CODE);
        when(reminderService.getByChatAndUser(message.getChat(), message.getUser(), 0)).thenReturn(new PageImpl<>(getReminderWithRepeatabilityList()));
        when(internationalizationService.internationalize(anyString(), anyString())).then(returnsFirstArg());

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        assertReminderInfoList(editResponse.getKeyboard().getKeyboardButtonsList());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCallbackWithAddArgumentTest() {
        final String expectedResponseText = "${command.remind.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestWithCallback("reminder add");
        Message message = request.getMessage();

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);

        assertEquals(expectedResponseText, editResponse.getText());

        verify(commandWaitingService).add(message.getChat(), message.getUser(), Remind.class, "remind ");
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCallbackWithCorruptedInfoCommandArgumentTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestWithCallback("reminder ia");
        Message message = request.getMessage();

        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> remind.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(botStats).incrementErrors(eq(message), any(NumberFormatException.class), eq("${command.remind.idparsingfail}"));
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCallbackWithAlreadyRemovedInfoCommandArgumentTest() {
        BotRequest request = TestUtils.getRequestWithCallback("reminder i1");
        Message message = request.getMessage();

        BotResponse botResponse = remind.parse(request).get(0);
        DeleteResponse deleteResponse = TestUtils.checkDefaultDeleteResponseParams(botResponse);

        assertEquals(message.getChat().getChatId(), deleteResponse.getChatId());
        assertEquals(message.getMessageId(), deleteResponse.getMessageId());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCallbackGetInfoCommandArgumentTest() {
        final String expectedResponseText = """
                <b>${command.remind.caption}</b>
                reminder1
                <i>31.12.1999 01:01:03 (Fri. )</i>
                ${command.remind.repeat}: <b>${command.remind.withoutrepeat}</b>
                ${command.remind.worked}: <b>1 ${utils.date.d}. 1 ${utils.date.m}.  (1 ${utils.date.d}. )</b>${command.remind.ago}
                """;
        final Long reminderId = 1L;
        BotRequest request = TestUtils.getRequestWithCallback("reminder i" + reminderId);
        Message message = request.getMessage();

        Reminder reminder = getNotifiedReminder();
        when(reminderService.get(reminderId)).thenReturn(reminder);
        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(USER_LANG_CODE);
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.withZone(any(ZoneId.class))).thenReturn(clock);

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);

        assertReminderInfoKeyboard(editResponse.getKeyboard().getKeyboardButtonsList(), reminder);

        assertEquals(expectedResponseText, editResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCallbackSetReminderWithCorruptedCommandTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestWithCallback("reminder sa");
        Message message = request.getMessage();

        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> remind.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(botStats).incrementErrors(message, "${command.remind.idparsingfail}");
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCallbackSetReminderWithCorruptedIdTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestWithCallback("reminder s" + Long.MAX_VALUE + "0");
        Message message = request.getMessage();

        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> remind.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(botStats).incrementErrors(eq(message), any(NumberFormatException.class), eq("${command.remind.idparsingfail}"));
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCallbackSetAlreadyRemovedReminderTest() {
        BotRequest request = TestUtils.getRequestWithCallback("reminder s1");
        Message message = request.getMessage();

        BotResponse botResponse = remind.parse(request).get(0);
        DeleteResponse deleteResponse = TestUtils.checkDefaultDeleteResponseParams(botResponse);

        assertEquals(message.getChat().getChatId(), deleteResponse.getChatId());
        assertEquals(message.getMessageId(), deleteResponse.getMessageId());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCallbackSetAnotherUsersReminderTest() {
        final Long reminderId = 1L;
        BotRequest request = TestUtils.getRequestWithCallback("reminder s" + reminderId);
        Message message = request.getMessage();

        Reminder changingReminder = getReminderWithRepeatabilityList().get(0);
        changingReminder.setUser(TestUtils.getUser(TestUtils.ANOTHER_USER_ID));
        when(reminderService.get(reminderId)).thenReturn(changingReminder);

        List<BotResponse> botResponses = remind.parse(request);
        assertTrue(botResponses.isEmpty());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCallbackSetReminderNotifiedTrueTest() {
        final String expectedResponseText = """
                <b>${command.remind.caption}</b>
                reminder3
                <i>01.01.2000 01:01:03 (сб. )</i>
                ${command.remind.repeat}: <b>command.remind.repeatability.monday, command.remind.repeatability.tuesday, command.remind.repeatability.wednesday, command.remind.repeatability.thursday, command.remind.repeatability.friday</b>
                ${command.remind.worked}: <b>1 ${utils.date.m}. </b>${command.remind.ago}
                """;
        final Long reminderId = 3L;
        BotRequest request = TestUtils.getRequestWithCallback("reminder s" + reminderId + "n");
        Message message = request.getMessage();

        Reminder changingReminder = getReminderWithRepeatability();
        when(reminderService.get(reminderId)).thenReturn(changingReminder);
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.withZone(any(ZoneId.class))).thenReturn(clock);

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        assertReminderInfoKeyboard(editResponse.getKeyboard().getKeyboardButtonsList(), changingReminder);

        verify(reminderService).save(changingReminder);
        assertEquals(true, changingReminder.getNotified());
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCallbackSetReminderNotifiedFalseTest() {
        final String expectedResponseText = """
                <b>${command.remind.caption}</b>
                reminder1
                <i>31.12.1999 01:01:03 (пт. )</i>
                ${command.remind.repeat}: <b>${command.remind.withoutrepeat}</b>
                ${command.remind.beforetriggering}: <b> ${command.remind.almostthere} </b>
                """;
        final Long reminderId = 1L;
        BotRequest request = TestUtils.getRequestWithCallback("reminder s" + reminderId + "n");
        Message message = request.getMessage();

        Reminder changingReminder = getNotifiedReminder();
        when(reminderService.get(reminderId)).thenReturn(changingReminder);
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.withZone(any(ZoneId.class))).thenReturn(clock);

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        assertReminderInfoKeyboard(editResponse.getKeyboard().getKeyboardButtonsList(), changingReminder);

        verify(reminderService).save(changingReminder);
        assertEquals(false, changingReminder.getNotified());
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCallbackSetRepeatableReminderNotifiedFalseTest() {
        final String expectedResponseText = """
                <b>${command.remind.caption}</b>
                reminder3
                <i>02.01.2000 01:27:03 (вс. )</i>
                ${command.remind.repeat}: <b>command.remind.repeatability.monday, command.remind.repeatability.tuesday, command.remind.repeatability.wednesday, command.remind.repeatability.thursday, command.remind.repeatability.friday</b>
                ${command.remind.beforetriggering}: <b>1 ${utils.date.d}. 25 ${utils.date.m}.  (1 ${utils.date.d}. )</b>
                """;
        final LocalDateTime expectedNextAlarmDateTime = LocalDateTime.of(CURRENT_DATE.plusDays(1), CURRENT_TIME.plusMinutes(25));
        final Long reminderId = 3L;
        BotRequest request = TestUtils.getRequestWithCallback("reminder s" + reminderId + "n");
        Message message = request.getMessage();

        Reminder changingReminder = getReminderWithRepeatability();
        changingReminder.setNotified(true);
        when(reminderService.get(reminderId)).thenReturn(changingReminder);
        when(reminderService.getNextAlarmDateTime(changingReminder)).thenReturn(expectedNextAlarmDateTime);
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.withZone(any(ZoneId.class))).thenReturn(clock);

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        assertReminderInfoKeyboard(editResponse.getKeyboard().getKeyboardButtonsList(), changingReminder);

        verify(reminderService).save(changingReminder);
        assertEquals(false, changingReminder.getNotified());
        assertEquals(expectedNextAlarmDateTime.toLocalDate(), changingReminder.getDate());
        assertEquals(expectedNextAlarmDateTime.toLocalTime(), changingReminder.getTime());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCallbackSetRepeatableTest() {
        final String expectedResponseText = """
            <b>${command.remind.caption}</b>
            reminder1
            <i>31.12.1999 01:01:03 (пт. )</i>
            ${command.remind.repeat}: <b>${command.remind.withoutrepeat}</b>
            ${command.remind.worked}: <b>1 ${utils.date.d}. 1 ${utils.date.m}.  (1 ${utils.date.d}. )</b>${command.remind.ago}
            <b>${command.remind.repeatevery}: </b>""";
        final Long reminderId = 1L;
        BotRequest request = TestUtils.getRequestWithCallback("reminder s" + reminderId + "r");
        Message message = request.getMessage();

        Reminder changingReminder = getNotifiedReminder();
        when(reminderService.get(reminderId)).thenReturn(changingReminder);
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.withZone(any(ZoneId.class))).thenReturn(clock);

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        verify(reminderService).save(changingReminder);
        verify(bot).sendTyping(message.getChatId());

        List<List<KeyboardButton>> keyboardButtonsList = editResponse.getKeyboard().getKeyboardButtonsList();
        assertEquals(6, keyboardButtonsList.size());

        List<KeyboardButton> minutesRow = keyboardButtonsList.get(0);
        assertEquals(5, minutesRow.size());

        KeyboardButton button1 = minutesRow.get(0);
        assertEquals("command.remind.repeatability.minutes1", button1.getName());
        assertEquals("remind s1r0", button1.getCallback());

        KeyboardButton button2 = minutesRow.get(1);
        assertEquals("command.remind.repeatability.minutes5", button2.getName());
        assertEquals("remind s1r1", button2.getCallback());

        KeyboardButton button3 = minutesRow.get(2);
        assertEquals("command.remind.repeatability.minutes10", button3.getName());
        assertEquals("remind s1r2", button3.getCallback());

        KeyboardButton button4 = minutesRow.get(3);
        assertEquals("command.remind.repeatability.minutes15", button4.getName());
        assertEquals("remind s1r3", button4.getCallback());

        KeyboardButton button5 = minutesRow.get(4);
        assertEquals("command.remind.repeatability.minutes30", button5.getName());
        assertEquals("remind s1r4", button5.getCallback());

        List<KeyboardButton> hoursRow = keyboardButtonsList.get(1);
        assertEquals(5, hoursRow.size());

        KeyboardButton button6 = hoursRow.get(0);
        assertEquals("command.remind.repeatability.hours1", button6.getName());
        assertEquals("remind s1r5", button6.getCallback());

        KeyboardButton button7 = hoursRow.get(1);
        assertEquals("command.remind.repeatability.hours2", button7.getName());
        assertEquals("remind s1r6", button7.getCallback());

        KeyboardButton button8 = hoursRow.get(2);
        assertEquals("command.remind.repeatability.hours3", button8.getName());
        assertEquals("remind s1r7", button8.getCallback());

        KeyboardButton button9 = hoursRow.get(3);
        assertEquals("command.remind.repeatability.hours6", button9.getName());
        assertEquals("remind s1r8", button9.getCallback());

        KeyboardButton button10 = hoursRow.get(4);
        assertEquals("command.remind.repeatability.hours12", button10.getName());
        assertEquals("remind s1r9", button10.getCallback());

        List<KeyboardButton> weekRow = keyboardButtonsList.get(2);
        assertEquals(7, weekRow.size());

        KeyboardButton button11 = weekRow.get(0);
        assertEquals("command.remind.repeatability.monday", button11.getName());
        assertEquals("remind s1r10", button11.getCallback());

        KeyboardButton button12 = weekRow.get(1);
        assertEquals("command.remind.repeatability.tuesday", button12.getName());
        assertEquals("remind s1r11", button12.getCallback());

        KeyboardButton button13 = weekRow.get(2);
        assertEquals("command.remind.repeatability.wednesday", button13.getName());
        assertEquals("remind s1r12", button13.getCallback());

        KeyboardButton button14 = weekRow.get(3);
        assertEquals("command.remind.repeatability.thursday", button14.getName());
        assertEquals("remind s1r13", button14.getCallback());

        KeyboardButton button15 = weekRow.get(4);
        assertEquals("command.remind.repeatability.friday", button15.getName());
        assertEquals("remind s1r14", button15.getCallback());

        KeyboardButton button16 = weekRow.get(5);
        assertEquals("command.remind.repeatability.saturday", button16.getName());
        assertEquals("remind s1r15", button16.getCallback());

        KeyboardButton button17 = weekRow.get(6);
        assertEquals("command.remind.repeatability.sunday", button17.getName());
        assertEquals("remind s1r16", button17.getCallback());

        List<KeyboardButton> othersRow = keyboardButtonsList.get(3);
        assertEquals(4, othersRow.size());

        KeyboardButton button18 = othersRow.get(0);
        assertEquals("command.remind.repeatability.day", button18.getName());
        assertEquals("remind s1r17", button18.getCallback());

        KeyboardButton button19 = othersRow.get(1);
        assertEquals("command.remind.repeatability.week", button19.getName());
        assertEquals("remind s1r18", button19.getCallback());

        KeyboardButton button20 = othersRow.get(2);
        assertEquals("command.remind.repeatability.month", button20.getName());
        assertEquals("remind s1r19", button20.getCallback());

        KeyboardButton button21 = othersRow.get(3);
        assertEquals("command.remind.repeatability.year", button21.getName());
        assertEquals("remind s1r20", button21.getCallback());

        assertControlRow(keyboardButtonsList.get(4), changingReminder);
        assertBackRow(keyboardButtonsList.get(5));
    }

    @Test
    void parseCallbackSetFullDateTest() {
        final String expectedResponseText = """
            <b>${command.remind.caption}</b>
            reminder1
            <i>01.01.2000 01:01:03 (сб. )</i>
            ${command.remind.repeat}: <b>${command.remind.withoutrepeat}</b>
            ${command.remind.worked}: <b>1 ${utils.date.m}. </b>${command.remind.ago}
            <b>${command.remind.timeset}
            </b>""";
        final String expectedCommandWaitingCommand = "remind s1d01.01.2000t";
        final Long reminderId = 1L;
        final LocalDate setDate = LocalDate.of(2000, 1, 1);
        BotRequest request = TestUtils.getRequestWithCallback("reminder s" + reminderId + "d" + DateUtils.formatDate(setDate));
        Message message = request.getMessage();

        Reminder changingReminder = getNotifiedReminder();
        when(reminderService.get(reminderId)).thenReturn(changingReminder);
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.withZone(any(ZoneId.class))).thenReturn(clock);

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        verify(reminderService).save(changingReminder);
        assertEquals(setDate, changingReminder.getDate());
        verify(commandWaitingService).add(message.getChat(), message.getUser(), Remind.class, expectedCommandWaitingCommand);
        verify(bot).sendTyping(message.getChatId());

        List<List<KeyboardButton>> keyboardButtonsList = editResponse.getKeyboard().getKeyboardButtonsList();
        assertSetTimeKeyboard(keyboardButtonsList, changingReminder);
        assertControlRow(keyboardButtonsList.get(6), changingReminder);
        assertBackRow(keyboardButtonsList.get(7));
    }

    @Test
    void parseCallbackSetFullDateTimeTest() {
        final String expectedResponseText = """
            <b>${command.remind.caption}</b>
            reminder1
            <i>01.01.2000 20:20:00 (сб. )</i>
            ${command.remind.repeat}: <b>${command.remind.withoutrepeat}</b>
            ${command.remind.beforetriggering}: <b>19 ${utils.date.h}. 17 ${utils.date.m}. 57 ${utils.date.s}. </b>
            <b></b>""";
        final Long reminderId = 1L;
        final LocalDate setDate = LocalDate.of(2000, 1, 1);
        final LocalTime setTime = LocalTime.of(20, 20);
        BotRequest request = TestUtils.getRequestWithCallback("reminder s" + reminderId + "d" + DateUtils.formatDate(setDate) + "t" + DateUtils.formatTime(setTime));
        Message message = request.getMessage();

        Reminder changingReminder = getNotifiedReminder();
        when(reminderService.get(reminderId)).thenReturn(changingReminder);
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.withZone(any(ZoneId.class))).thenReturn(clock);

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        verify(reminderService).save(changingReminder);
        assertEquals(setDate, changingReminder.getDate());
        assertEquals(setTime, changingReminder.getTime());
        assertFalse(changingReminder.getNotified());
        verify(bot).sendTyping(message.getChatId());

        List<List<KeyboardButton>> keyboardButtonsList = editResponse.getKeyboard().getKeyboardButtonsList();
        assertEquals(2, keyboardButtonsList.size());

        assertControlRow(keyboardButtonsList.get(0), changingReminder);
        assertBackRow(keyboardButtonsList.get(1));
    }

    @Test
    void parseCallbackSetPostponeTest() {
        final String expectedResponseText = """
            <b>${command.remind.caption}</b>
            reminder1
            <i>01.01.2000 01:07:03 (сб. )</i>
            ${command.remind.repeat}: <b>${command.remind.withoutrepeat}</b>
            ${command.remind.beforetriggering}: <b>5 ${utils.date.m}. </b>
            <b></b>""";
        final Long reminderId = 1L;
        Duration duration = Duration.ofMinutes(5);
        BotRequest request = TestUtils.getRequestWithCallback("reminder s" + reminderId + duration);
        Message message = request.getMessage();

        Reminder changingReminder = getNotifiedReminder();
        when(reminderService.get(reminderId)).thenReturn(changingReminder);
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.withZone(any(ZoneId.class))).thenReturn(clock);

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        verify(reminderService).save(changingReminder);
        assertEquals(CURRENT_DATE, changingReminder.getDate());
        assertEquals(CURRENT_TIME.plus(duration), changingReminder.getTime());
        assertFalse(changingReminder.getNotified());
        verify(bot).sendTyping(message.getChatId());

        List<List<KeyboardButton>> keyboardButtonsList = editResponse.getKeyboard().getKeyboardButtonsList();
        assertEquals(2, keyboardButtonsList.size());

        assertControlRow(keyboardButtonsList.get(0), changingReminder);
        assertBackRow(keyboardButtonsList.get(1));
    }

    @Test
    void parseCallbackSetPostponeRepeatabilityReminderTest() {
        Reminder changingReminder = getReminderWithRepeatability();

        final String expectedResponseText = """
            <b>${command.remind.caption}</b>
            (${command.remind.copy}) reminder3
            <i>01.01.2000 01:07:03 (сб. )</i>
            ${command.remind.repeat}: <b>${command.remind.withoutrepeat}</b>
            ${command.remind.beforetriggering}: <b>5 ${utils.date.m}. </b>
            <b></b>""";
        final Long reminderId = changingReminder.getId();
        final String newReminderText = "(${command.remind.copy}) reminder3";
        Duration duration = Duration.ofMinutes(5);
        BotRequest request = TestUtils.getRequestWithCallback("reminder s" + reminderId + duration);
        Message message = request.getMessage();

        when(reminderService.get(reminderId)).thenReturn(changingReminder);
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.withZone(any(ZoneId.class))).thenReturn(clock);

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        ArgumentCaptor<Reminder> reminderArgumentCaptor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderService).save(reminderArgumentCaptor.capture());

        Reminder savedReminder = reminderArgumentCaptor.getValue();
        assertEquals(message.getUser(), savedReminder.getUser());
        assertEquals(message.getChat(), savedReminder.getChat());
        assertEquals(CURRENT_DATE, savedReminder.getDate());
        assertEquals(CURRENT_TIME.plus(duration), savedReminder.getTime());
        assertEquals(newReminderText, savedReminder.getText());
        assertFalse(savedReminder.getNotified());

        verify(bot).sendTyping(message.getChatId());

        List<List<KeyboardButton>> keyboardButtonsList = editResponse.getKeyboard().getKeyboardButtonsList();
        assertEquals(2, keyboardButtonsList.size());
    }

    @Test
    void parseCallbackSetDateTest() {
        final String expectedResponseText = """
                <b>${command.remind.caption}</b>
                reminder1
                <i>31.12.1999 01:01:03 (пт. )</i>
                ${command.remind.repeat}: <b>${command.remind.withoutrepeat}</b>
                ${command.remind.worked}: <b>1 ${utils.date.d}. 1 ${utils.date.m}.  (1 ${utils.date.d}. )</b>${command.remind.ago}
                <b>${command.remind.dateset}
                </b>""";
        final String expectedCommandWaitingCommand = "remind s1d";
        final Long reminderId = 1L;
        BotRequest request = TestUtils.getRequestWithCallback("reminder s" + reminderId);
        Message message = request.getMessage();

        Reminder changingReminder = getNotifiedReminder();
        when(reminderService.get(reminderId)).thenReturn(changingReminder);
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.withZone(any(ZoneId.class))).thenReturn(clock);

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        verify(reminderService).save(changingReminder);
        verify(commandWaitingService).add(message.getChat(), message.getUser(), Remind.class, expectedCommandWaitingCommand);
        verify(bot).sendTyping(message.getChatId());

        List<List<KeyboardButton>> keyboardButtonsList = editResponse.getKeyboard().getKeyboardButtonsList();
        assertEquals(8, keyboardButtonsList.size());

        List<KeyboardButton> row1 = keyboardButtonsList.get(0);
        assertEquals(1, row1.size());
        KeyboardButton button1 = row1.get(0);
        assertEquals("${command.remind.leave} 31.12.1999", button1.getName());
        assertEquals("remind s1d31.12.1999", button1.getCallback());

        List<KeyboardButton> row2 = keyboardButtonsList.get(1);
        assertEquals(1, row2.size());
        KeyboardButton button2 = row2.get(0);
        assertEquals("${command.remind.today}", button2.getName());
        assertEquals("remind s1d01.01.2000", button2.getCallback());

        List<KeyboardButton> row3 = keyboardButtonsList.get(2);
        assertEquals(1, row3.size());
        KeyboardButton button3 = row3.get(0);
        assertEquals("${command.remind.tomorrow}", button3.getName());
        assertEquals("remind s1d02.01.2000", button3.getCallback());

        List<KeyboardButton> row4 = keyboardButtonsList.get(3);
        assertEquals(1, row4.size());
        KeyboardButton button4 = row4.get(0);
        assertEquals("${command.remind.aftertomorrow}", button4.getName());
        assertEquals("remind s1d03.01.2000", button4.getCallback());

        List<KeyboardButton> row5 = keyboardButtonsList.get(4);
        assertEquals(1, row5.size());
        KeyboardButton button5 = row5.get(0);
        assertEquals("${command.remind.onsaturday}", button5.getName());
        assertEquals("remind s1d08.01.2000", button5.getCallback());

        List<KeyboardButton> row6 = keyboardButtonsList.get(5);
        assertEquals(1, row6.size());
        KeyboardButton button6 = row6.get(0);
        assertEquals("${command.remind.onsunday}", button6.getName());
        assertEquals("remind s1d02.01.2000", button6.getCallback());

        assertControlRow(keyboardButtonsList.get(6), changingReminder);
        assertBackRow(keyboardButtonsList.get(7));
    }

    @Test
    void deleteReminderByCallbackEmptyParamsTest() {
        final String expectedResponseText = """
            <a href="tg://user?id=1">username</a><b> ${command.remind.yourreminders}:</b>
            31.12 01:01 (Fri. ) — reminder1 🔕
            01.01 01:02 (Sat. ) — reminder2 with very very very long text 🔔
            01.01 01:01 (Sat. ) — reminder3 🔔📆""";
        BotRequest request = TestUtils.getRequestWithCallback("remind del");
        Message message = request.getMessage();

        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(USER_LANG_CODE);
        when(reminderService.getByChatAndUser(message.getChat(), message.getUser(), 0)).thenReturn(new PageImpl<>(getReminderWithRepeatabilityList()));
        when(internationalizationService.internationalize(anyString(), anyString())).then(returnsFirstArg());

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);

        assertEquals(expectedResponseText, editResponse.getText());

        assertReminderInfoList(editResponse.getKeyboard().getKeyboardButtonsList());
    }

    @Test
    void deleteReminderByCallbackCurruptedIdTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestWithCallback("remind dela");

        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> remind.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(botStats).incrementErrors(any(Message.class), any(NumberFormatException.class), anyString());
    }

    @Test
    void deleteReminderByCallbackSomeOneElseReminderTest() {
        final Long reminderId = 1L;
        BotRequest request = TestUtils.getRequestWithCallback("remind del" + reminderId);

        Reminder reminder = getNotifiedReminder();
        reminder.setUser(TestUtils.getUser(TestUtils.ANOTHER_USER_ID));
        when(reminderService.get(reminderId)).thenReturn(reminder);

        List<BotResponse> botResponses = remind.parse(request);
        assertTrue(botResponses.isEmpty());
    }

    @Test
    void deleteReminderByCallbackAlreadyDeletedTest() {
        BotRequest request = TestUtils.getRequestWithCallback("remind del1");

        BotResponse botResponse = remind.parse(request).get(0);
        TestUtils.checkDefaultDeleteResponseParams(botResponse);
    }

    @Test
    void deleteReminderByCallbackWithMessageDeletingTest() {
        final Long reminderId = 1L;
        BotRequest request = TestUtils.getRequestWithCallback("remind del" + reminderId + "c");

        Reminder reminder = getNotifiedReminder();
        when(reminderService.get(reminderId)).thenReturn(reminder);

        BotResponse botResponse = remind.parse(request).get(0);
        TestUtils.checkDefaultDeleteResponseParams(botResponse);

        verify(reminderService).remove(reminder);
    }

    @Test
    void deleteReminderByCallbackTest() {
        final String expectedResponseText = """
            <a href="tg://user?id=1">username</a><b> ${command.remind.yourreminders}:</b>
            31.12 01:01 (Fri. ) — reminder1 🔕
            01.01 01:02 (Sat. ) — reminder2 with very very very long text 🔔
            01.01 01:01 (Sat. ) — reminder3 🔔📆""";
        final Long reminderId = 1L;
        BotRequest request = TestUtils.getRequestWithCallback("remind del" + reminderId);
        Message message = request.getMessage();

        Reminder reminder = getNotifiedReminder();
        when(reminderService.get(reminderId)).thenReturn(reminder);
        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(USER_LANG_CODE);
        when(reminderService.getByChatAndUser(message.getChat(), message.getUser(), 0)).thenReturn(new PageImpl<>(getReminderWithRepeatabilityList()));
        when(internationalizationService.internationalize(anyString(), anyString())).then(returnsFirstArg());

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);

        assertEquals(expectedResponseText, editResponse.getText());

        assertReminderInfoList(editResponse.getKeyboard().getKeyboardButtonsList());
    }

    @Test
    void selectPageCorruptedNumberTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestWithCallback("remind pagea");

        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> remind.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(botStats).incrementErrors(anyString(), any(NumberFormatException.class), anyString());
    }

    @Test
    void selectPageTest() {
        final String expectedResponseText = """
            <a href="tg://user?id=1">username</a><b> ${command.remind.yourreminders}:</b>
            31.12 01:01 (Fri. ) — reminder1 🔕
            01.01 01:02 (Sat. ) — reminder2 with very very very long text 🔔
            01.01 01:01 (Sat. ) — reminder3 🔔📆""";
        BotRequest request = TestUtils.getRequestWithCallback("remind page0");
        Message message = request.getMessage();

        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(USER_LANG_CODE);
        when(reminderService.getByChatAndUser(message.getChat(), message.getUser(), 0)).thenReturn(new PageImpl<>(getReminderWithRepeatabilityList()));
        when(internationalizationService.internationalize(anyString(), anyString())).then(returnsFirstArg());

        BotResponse botResponse = remind.parse(request).get(0);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);

        assertEquals(expectedResponseText, editResponse.getText());

        assertReminderInfoList(editResponse.getKeyboard().getKeyboardButtonsList());
    }

    @Test
    void closeReminderMenuSomeOneElseTest() {
        BotRequest request = TestUtils.getRequestWithCallback("remind c" + TestUtils.ANOTHER_USER_ID);
        List<BotResponse> botResponses = remind.parse(request);
        assertTrue(botResponses.isEmpty());
    }

    @Test
    void closeReminderMenuTest() {
        BotRequest request = TestUtils.getRequestWithCallback("remind c" + TestUtils.DEFAULT_USER_ID);
        BotResponse botResponse = remind.parse(request).get(0);
        TestUtils.checkDefaultDeleteResponseParams(botResponse);
    }

    @Test
    void unknownCallbackCommandTest() {
        BotRequest request = TestUtils.getRequestWithCallback("remind abv");
        List<BotResponse> botResponses = remind.parse(request);
        assertTrue(botResponses.isEmpty());
        verify(botStats).incrementErrors(any(BotRequest.class), anyString());
    }

    @Test
    void parseWithoutArgumentsTest() {
        final String expectedResponseText = """
            <a href="tg://user?id=1">username</a><b> ${command.remind.yourreminders}:</b>
            31.12 01:01 (Fri. ) — reminder1 🔕
            01.01 01:02 (Sat. ) — reminder2 with very very very long text 🔔
            01.01 01:01 (Sat. ) — reminder3 🔔📆""";
        BotRequest request = TestUtils.getRequestFromGroup("remind");
        Message message = request.getMessage();

        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(USER_LANG_CODE);
        when(reminderService.getByChatAndUser(message.getChat(), message.getUser(), 0)).thenReturn(new PageImpl<>(getReminderWithRepeatabilityList()));
        when(internationalizationService.internationalize(anyString(), anyString())).then(returnsFirstArg());

        BotResponse botResponse = remind.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        assertReminderInfoList(textResponse.getKeyboard().getKeyboardButtonsList());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void manualEditEmptyArgumentTest() {
        final String expectedErrorMessage = "error";
        BotRequest request = TestUtils.getRequestFromGroup("s");
        Message message = request.getMessage();

        CommandWaiting commandWaiting = mock(CommandWaiting.class);
        when(commandWaiting.getCommandName()).thenReturn("remind");
        when(commandWaiting.getTextMessage()).thenReturn("remind ");
        when(commandWaitingService.get(message.getChat(), message.getUser())).thenReturn(commandWaiting);
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> remind.parse(request));
        assertEquals(expectedErrorMessage, botException.getMessage());

        verify(commandWaitingService).remove(commandWaiting);
        verify(reminderService, never()).save(any(Reminder.class));
    }

    @Test
    void manualEditCorruptedReminderIdTest() {
        final String expectedErrorMessage = "error";
        BotRequest request = TestUtils.getRequestFromGroup("s" + Long.MAX_VALUE + "0");
        Message message = request.getMessage();

        CommandWaiting commandWaiting = mock(CommandWaiting.class);
        when(commandWaiting.getCommandName()).thenReturn("remind");
        when(commandWaiting.getTextMessage()).thenReturn("remind ");
        when(commandWaitingService.get(message.getChat(), message.getUser())).thenReturn(commandWaiting);
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> remind.parse(request));
        assertEquals(expectedErrorMessage, botException.getMessage());

        verify(commandWaitingService).remove(commandWaiting);
        verify(reminderService, never()).save(any(Reminder.class));
    }

    @Test
    void manualEditNotFoundReminderTest() {
        final String expectedErrorMessage = "error";
        BotRequest request = TestUtils.getRequestFromGroup("s1");
        Message message = request.getMessage();

        CommandWaiting commandWaiting = mock(CommandWaiting.class);
        when(commandWaiting.getCommandName()).thenReturn("remind");
        when(commandWaiting.getTextMessage()).thenReturn("remind ");
        when(commandWaitingService.get(message.getChat(), message.getUser())).thenReturn(commandWaiting);
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> remind.parse(request));
        assertEquals(expectedErrorMessage, botException.getMessage());

        verify(commandWaitingService).remove(commandWaiting);
        verify(reminderService, never()).save(any(Reminder.class));
    }

    @Test
    void manualEditButNothingToSetTest() {
        final String expectedErrorMessage = "error";
        final Long reminderId = 1L;
        BotRequest request = TestUtils.getRequestFromGroup("s" + reminderId);
        Message message = request.getMessage();

        CommandWaiting commandWaiting = mock(CommandWaiting.class);
        when(commandWaiting.getCommandName()).thenReturn("remind");
        when(commandWaiting.getTextMessage()).thenReturn("remind ");
        when(commandWaitingService.get(message.getChat(), message.getUser())).thenReturn(commandWaiting);
        when(reminderService.get(message.getChat(), message.getUser(), reminderId)).thenReturn(getNotifiedReminder());
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> remind.parse(request));
        assertEquals(expectedErrorMessage, botException.getMessage());

        verify(commandWaitingService).remove(commandWaiting);
        verify(reminderService, never()).save(any(Reminder.class));
    }

    @Test
    void manualEditSetFullDateTest() {
        final String expectedResponse = """
                <b>${command.remind.caption}</b>
                reminder1
                <i>23.01.2024 00:00:00 (Tue. )</i>
                ${command.remind.repeat}: <b>${command.remind.withoutrepeat}</b>
                ${command.remind.disabled}
                <b>${command.remind.timeset}
                </b>""";
        final String expectedCommandWaitingCommand = "remind s1d23.01.2024t";
        final Long reminderId = 1L;
        final LocalDate setDate = LocalDate.of(2024, 1, 23);
        BotRequest request = TestUtils.getRequestFromGroup("s" + reminderId + "d" + DateUtils.formatDate(setDate));
        Message message = request.getMessage();

        CommandWaiting commandWaiting = mock(CommandWaiting.class);
        when(commandWaiting.getCommandName()).thenReturn("remind");
        when(commandWaiting.getTextMessage()).thenReturn("remind ");
        when(commandWaitingService.get(message.getChat(), message.getUser())).thenReturn(commandWaiting);
        Reminder reminder = getNotifiedReminder();
        when(reminderService.get(message.getChat(), message.getUser(), reminderId)).thenReturn(reminder);
        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(USER_LANG_CODE);
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.withZone(any(ZoneId.class))).thenReturn(clock);

        BotResponse botResponse = remind.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponse, textResponse.getText());

        List<List<KeyboardButton>> keyboardButtonsList = textResponse.getKeyboard().getKeyboardButtonsList();
        assertSetTimeKeyboard(keyboardButtonsList, reminder);
        assertControlRow(keyboardButtonsList.get(6), reminder);
        assertBackRow(keyboardButtonsList.get(7));

        assertEquals(setDate, reminder.getDate());
        assertEquals(LocalTime.MIN, reminder.getTime());

        verify(commandWaitingService).remove(commandWaiting);
        verify(commandWaitingService).add(message.getChat(), message.getUser(), Remind.class, expectedCommandWaitingCommand);
        verify(reminderService).save(any(Reminder.class));
    }

    @Test
    void manualEditSetTimeButNothingToSetTest() {
        final String expectedErrorMessage = "error";
        final Long reminderId = 1L;
        BotRequest request = TestUtils.getRequestFromGroup("s" + reminderId + "d23.01.2024t");
        Message message = request.getMessage();

        CommandWaiting commandWaiting = mock(CommandWaiting.class);
        when(commandWaiting.getCommandName()).thenReturn("remind");
        when(commandWaiting.getTextMessage()).thenReturn("remind ");
        when(commandWaitingService.get(message.getChat(), message.getUser())).thenReturn(commandWaiting);
        when(reminderService.get(message.getChat(), message.getUser(), reminderId)).thenReturn(getNotifiedReminder());
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> remind.parse(request));
        assertEquals(expectedErrorMessage, botException.getMessage());

        verify(commandWaitingService).remove(commandWaiting);
        verify(reminderService, never()).save(any(Reminder.class));
    }

    @Test
    void manualEditSetShortDateAndTimeTest() {
        final String expectedResponse = """
            <b>${command.remind.caption}</b>
            reminder1
            <i>23.01.2000 15:15:00 (Sun. )</i>
            ${command.remind.repeat}: <b>${command.remind.withoutrepeat}</b>
            ${command.remind.beforetriggering}: <b>22 ${utils.date.d}. 14 ${utils.date.h}. 12 ${utils.date.m}. 57 ${utils.date.s}.  (22 ${utils.date.d}. )</b>
            <b></b>""";
        final Long reminderId = 1L;
        final LocalDate setDate = LocalDate.of(2000, 1, 23);
        final LocalTime setTime = LocalTime.of(15, 15);
        BotRequest request = TestUtils.getRequestFromGroup("s" + reminderId
                + "d" + setDate.getDayOfMonth() + ".0" + setDate.getMonthValue()
                + "t" + DateUtils.formatTime(setTime));
        Message message = request.getMessage();

        CommandWaiting commandWaiting = mock(CommandWaiting.class);
        when(commandWaiting.getCommandName()).thenReturn("remind");
        when(commandWaiting.getTextMessage()).thenReturn("remind ");
        when(commandWaitingService.get(message.getChat(), message.getUser())).thenReturn(commandWaiting);
        Reminder reminder = getNotifiedReminder();
        when(reminderService.get(message.getChat(), message.getUser(), reminderId)).thenReturn(reminder);
        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn(USER_LANG_CODE);
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.withZone(any(ZoneId.class))).thenReturn(clock);

        BotResponse botResponse = remind.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponse, textResponse.getText());

        List<List<KeyboardButton>> keyboardButtonsList = textResponse.getKeyboard().getKeyboardButtonsList();
        assertControlRow(keyboardButtonsList.get(0), reminder);
        assertBackRow(keyboardButtonsList.get(1));

        assertEquals(setDate, reminder.getDate());
        assertEquals(setTime, reminder.getTime());
        assertFalse(reminder.getNotified());

        verify(commandWaitingService).remove(commandWaiting);
        verify(commandWaitingService, never()).add(any(Chat.class), any(User.class), any(Class.class), anyString());
        verify(reminderService).save(any(Reminder.class));
    }

    @ParameterizedTest
    @MethodSource("provideAddingReminderCommands")
    void addEmptyReminderTest(String inputCommand, LocalDate expectedDate, LocalTime expectedTime) {
        final String reminderText = "test";
        BotRequest request = TestUtils.getRequestFromGroup("reminder " + reminderText + inputCommand);
        Message message = request.getMessage();

        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.withZone(any(ZoneId.class))).thenReturn(clock);
        when(reminderService.save(any(Reminder.class))).then(answer -> answer.getArgument(0));

        when(internationalizationService.getAllTranslations("command.remind.in")).thenReturn(Set.of("in", "через"));
        when(internationalizationService.getAllTranslations("command.remind.minutes")).thenReturn(Set.of("minute#minutes", "минуту#минуты#минут"));
        when(internationalizationService.getAllTranslations("command.remind.hours")).thenReturn(Set.of("hour#hours", "час#часа#часов"));
        when(internationalizationService.getAllTranslations("command.remind.days")).thenReturn(Set.of("day#days", "день#дня#дней"));

        when(internationalizationService.getAllTranslations("command.remind.datekeyword.today")).thenReturn(Set.of("today", "сегодня"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.aftertomorrow")).thenReturn(Set.of("after tomorrow", "послезавтра"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.tomorrow")).thenReturn(Set.of("tomorrow", "завтра"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.monday")).thenReturn(Set.of("on Monday#on monday", "в понедельник"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.tuesday")).thenReturn(Set.of("on Tuesday#on tuesday", "во вторник"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.wednesday")).thenReturn(Set.of("on Wednesday#on wednessday", "в среду"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.thursday")).thenReturn(Set.of("on Thursday#on thursday", "в четверг"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.friday")).thenReturn(Set.of("on Friday#on friday", "в пятницу"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.saturday")).thenReturn(Set.of("on Saturday#on saturday", "в субботу"));
        when(internationalizationService.getAllTranslations("command.remind.datekeyword.sunday")).thenReturn(Set.of("on Sunday#on sunday", "в воскресенье"));

        when(internationalizationService.getAllTranslations("command.remind.timekeyword.morning")).thenReturn(Set.of("morning", "утром"));
        when(internationalizationService.getAllTranslations("command.remind.timekeyword.lunch")).thenReturn(Set.of("lunch", "в обед#к обеду"));
        when(internationalizationService.getAllTranslations("command.remind.timekeyword.afternoon")).thenReturn(Set.of("afternoon", "днём#днем"));
        when(internationalizationService.getAllTranslations("command.remind.timekeyword.evening")).thenReturn(Set.of("evening", "поздним вечером#поздно вечером"));
        when(internationalizationService.getAllTranslations("command.remind.timekeyword.dinner")).thenReturn(Set.of("dinner", "вечером"));
        when(internationalizationService.getAllTranslations("command.remind.timekeyword.night")).thenReturn(Set.of("night", "ночью"));

        ReflectionTestUtils.invokeMethod(remind, "postConstruct");

        BotResponse botResponse = remind.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        ArgumentCaptor<Reminder> reminderArgumentCaptor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderService).save(reminderArgumentCaptor.capture());

        Reminder savedReminder = reminderArgumentCaptor.getValue();
        assertEquals(message.getChat(), savedReminder.getChat());
        assertEquals(message.getUser(), savedReminder.getUser());
        assertEquals(expectedDate, savedReminder.getDate());
        assertEquals(expectedTime, savedReminder.getTime());
        assertEquals(reminderText, savedReminder.getText());
        assertFalse(savedReminder.getNotified());

        assertReminderInfoKeyboard(textResponse.getKeyboard().getKeyboardButtonsList(), savedReminder);
    }

    private static Stream<Arguments> provideAddingReminderCommands() {
        return Stream.of(
                Arguments.of("", CURRENT_DATE.plusDays(1), LocalTime.MIN),
                Arguments.of("02.03", LocalDate.of(2000, 3, 2), LocalTime.MIN),
                Arguments.of("02.03.2000", LocalDate.of(2000, 3, 2), LocalTime.MIN),
                Arguments.of("20:20", CURRENT_DATE, LocalTime.of(20, 20)),
                Arguments.of(" in 3 minutes", CURRENT_DATE, CURRENT_TIME.plusMinutes(3)),
                Arguments.of(" in 3 hours", CURRENT_DATE, CURRENT_TIME.plusHours(3)),
                Arguments.of(" in 3 days", CURRENT_DATE.plusDays(3), LocalTime.MIN),
                Arguments.of("morning", CURRENT_DATE, LocalTime.of(7, 0)),
                Arguments.of("tomorrow", CURRENT_DATE.plusDays(1), LocalTime.MIN)
        );
    }

    @Test
    void prepareTextOfReminderTest() {
        final String expected = """
                <b>${command.remind.caption}</b>
                reminder1
                <i>31.12.1999 01:01:03</i>
                ${command.remind.postponeuntil}:
                """;
        Reminder reminder = getNotifiedReminder();

        String actual = remind.prepareTextOfReminder(reminder);

        assertEquals(expected, actual);
    }

    @Test
    void preparePostponeKeyboardTest() {
        Reminder reminder = getNotifiedReminder();

        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(clock.withZone(any(ZoneId.class))).thenReturn(clock);

        Keyboard keyboard = remind.preparePostponeKeyboard(reminder, ZoneId.systemDefault(), Locale.ENGLISH);

        List<List<KeyboardButton>> keyboardButtonsList = keyboard.getKeyboardButtonsList();
        assertEquals(5, keyboardButtonsList.size());

        List<KeyboardButton> minutesRow = keyboardButtonsList.get(0);
        assertEquals(5, minutesRow.size());

        KeyboardButton button1 = minutesRow.get(0);
        assertEquals("1 ${command.remind.m}.", button1.getName());
        assertEquals("remind s1PT1M", button1.getCallback());

        KeyboardButton button2 = minutesRow.get(1);
        assertEquals("5 ${command.remind.m}.", button2.getName());
        assertEquals("remind s1PT5M", button2.getCallback());

        KeyboardButton button3 = minutesRow.get(2);
        assertEquals("10 ${command.remind.m}.", button3.getName());
        assertEquals("remind s1PT10M", button3.getCallback());

        KeyboardButton button4 = minutesRow.get(3);
        assertEquals("15 ${command.remind.m}.", button4.getName());
        assertEquals("remind s1PT15M", button4.getCallback());

        KeyboardButton button5 = minutesRow.get(4);
        assertEquals("30 ${command.remind.m}.", button5.getName());
        assertEquals("remind s1PT30M", button5.getCallback());

        List<KeyboardButton> hoursRow = keyboardButtonsList.get(1);
        assertEquals(5, hoursRow.size());

        KeyboardButton button6 = hoursRow.get(0);
        assertEquals("1 ${command.remind.h}.", button6.getName());
        assertEquals("remind s1PT1H", button6.getCallback());

        KeyboardButton button7 = hoursRow.get(1);
        assertEquals("2 ${command.remind.h}.", button7.getName());
        assertEquals("remind s1PT2H", button7.getCallback());

        KeyboardButton button8 = hoursRow.get(2);
        assertEquals("3 ${command.remind.h}.", button8.getName());
        assertEquals("remind s1PT3H", button8.getCallback());

        KeyboardButton button9 = hoursRow.get(3);
        assertEquals("6 ${command.remind.h}.", button9.getName());
        assertEquals("remind s1PT6H", button9.getCallback());

        KeyboardButton button10 = hoursRow.get(4);
        assertEquals("12 ${command.remind.h}.", button10.getName());
        assertEquals("remind s1PT12H", button10.getCallback());

        List<KeyboardButton> weekDayRow = keyboardButtonsList.get(2);
        assertEquals(7, weekDayRow.size());

        KeyboardButton button11 = weekDayRow.get(0);
        assertEquals("Mon", button11.getName());
        assertEquals("remind s1P2D", button11.getCallback());

        KeyboardButton button12 = weekDayRow.get(1);
        assertEquals("Tue", button12.getName());
        assertEquals("remind s1P3D", button12.getCallback());

        KeyboardButton button13 = weekDayRow.get(2);
        assertEquals("Wed", button13.getName());
        assertEquals("remind s1P4D", button13.getCallback());

        KeyboardButton button14 = weekDayRow.get(3);
        assertEquals("Thu", button14.getName());
        assertEquals("remind s1P5D", button14.getCallback());

        KeyboardButton button15 = weekDayRow.get(4);
        assertEquals("Fri", button15.getName());
        assertEquals("remind s1P6D", button15.getCallback());

        KeyboardButton button16 = weekDayRow.get(5);
        assertEquals("Sat", button16.getName());
        assertEquals("remind s1P7D", button16.getCallback());

        KeyboardButton button17 = weekDayRow.get(6);
        assertEquals("Sun", button17.getName());
        assertEquals("remind s1P1D", button17.getCallback());

        List<KeyboardButton> othersRow = keyboardButtonsList.get(3);
        assertEquals(4, othersRow.size());

        KeyboardButton button18 = othersRow.get(0);
        assertEquals("${command.remind.day}", button18.getName());
        assertEquals("remind s1P1D", button18.getCallback());

        KeyboardButton button19 = othersRow.get(1);
        assertEquals("${command.remind.week}", button19.getName());
        assertEquals("remind s1P7D", button19.getCallback());

        KeyboardButton button20 = othersRow.get(2);
        assertEquals("${command.remind.month}", button20.getName());
        assertEquals("remind s1P1M", button20.getCallback());

        KeyboardButton button21 = othersRow.get(3);
        assertEquals("${command.remind.year}", button21.getName());
        assertEquals("remind s1P1Y", button21.getCallback());

        assertControlRow(keyboardButtonsList.get(4), reminder, true);
    }

    private void assertReminderInfoKeyboard(List<List<KeyboardButton>> keyboardButtonsList, Reminder reminder) {
        assertControlRow(keyboardButtonsList.get(0), reminder);
        assertBackRow(keyboardButtonsList.get(1));
    }

    private void assertSetTimeKeyboard(List<List<KeyboardButton>> keyboardButtonsList, Reminder reminder) {
        final String setDateString = "remind s" + reminder.getId() + "d" + DateUtils.formatDate(reminder.getDate());

        assertEquals(8, keyboardButtonsList.size());

        List<KeyboardButton> row1 = keyboardButtonsList.get(0);
        assertEquals(1, row1.size());
        KeyboardButton button1 = row1.get(0);
        assertEquals("${command.remind.leave} 01:01", button1.getName());
        assertEquals(setDateString + "t01:01", button1.getCallback());

        List<KeyboardButton> row2 = keyboardButtonsList.get(1);
        assertEquals(1, row2.size());
        KeyboardButton button2 = row2.get(0);
        assertEquals("${command.remind.morning} 07:00", button2.getName());
        assertEquals(setDateString + "t07:00", button2.getCallback());

        List<KeyboardButton> row3 = keyboardButtonsList.get(2);
        assertEquals(1, row3.size());
        KeyboardButton button3 = row3.get(0);
        assertEquals("${command.remind.lunch} 13:00", button3.getName());
        assertEquals(setDateString + "t13:00", button3.getCallback());

        List<KeyboardButton> row4 = keyboardButtonsList.get(3);
        assertEquals(1, row4.size());
        KeyboardButton button4 = row4.get(0);
        assertEquals("${command.remind.dinner} 18:00", button4.getName());
        assertEquals(setDateString + "t18:00", button4.getCallback());

        List<KeyboardButton> row5 = keyboardButtonsList.get(4);
        assertEquals(1, row5.size());
        KeyboardButton button5 = row5.get(0);
        assertEquals("${command.remind.evening} 20:00", button5.getName());
        assertEquals(setDateString + "t20:00", button5.getCallback());

        List<KeyboardButton> row6 = keyboardButtonsList.get(5);
        assertEquals(1, row6.size());
        KeyboardButton button6 = row6.get(0);
        assertEquals("${command.remind.night} 03:00", button6.getName());
        assertEquals(setDateString + "t03:00", button6.getCallback());
    }

    private void assertControlRow(List<KeyboardButton> controlRow, Reminder reminder) {
        assertControlRow(controlRow, reminder, false);
    }

    private void assertControlRow(List<KeyboardButton> controlRow, Reminder reminder, boolean fromPostponeMenu) {
        Long id = reminder.getId();
        Boolean notified = reminder.getNotified();
        String repeatability = reminder.getRepeatability();
        if (repeatability == null) {
            repeatability = "";
        }

        assertEquals(6, controlRow.size());

        KeyboardButton setButton = controlRow.get(0);
        assertEquals("⚙\uFE0F", setButton.getName());
        assertEquals("remind s" + id, setButton.getCallback());

        KeyboardButton deleteButton = controlRow.get(1);
        assertEquals("❌", deleteButton.getName());
        if (fromPostponeMenu) {
            assertEquals("remind del" + id + "c", deleteButton.getCallback());
        } else {
            assertEquals("remind del" + id, deleteButton.getCallback());
        }

        String notifiedButtonText;
        if (notified) {
            notifiedButtonText = "▶";
        } else {
            notifiedButtonText = "⏹";
        }
        KeyboardButton notifiedButton = controlRow.get(2);
        assertEquals(notifiedButtonText, notifiedButton.getName());
        assertEquals("remind s" + id + "n", notifiedButton.getCallback());

        KeyboardButton repeatableButton = controlRow.get(3);
        assertEquals("\uD83D\uDCC6", repeatableButton.getName());
        assertEquals("remind s" + id + "r" + repeatability, repeatableButton.getCallback());

        KeyboardButton reloadButton = controlRow.get(4);
        assertEquals("\uD83D\uDD04", reloadButton.getName());
        assertEquals("remind i" + id, reloadButton.getCallback());

        KeyboardButton okButton = controlRow.get(5);
        assertEquals("✅", okButton.getName());
        assertEquals("remind c1", okButton.getCallback());
    }

    private void assertBackRow(List<KeyboardButton> backRow) {
        assertEquals(1, backRow.size());
        KeyboardButton backButton = backRow.get(0);
        assertEquals("⬅\uFE0F${command.remind.button.back}", backButton.getName());
        assertEquals("remind ", backButton.getCallback());
    }

    private void assertReminderInfoList(List<List<KeyboardButton>> keyboardButtonsList) {
        assertEquals(5, keyboardButtonsList.size());

        List<KeyboardButton> row1 = keyboardButtonsList.get(0);
        assertEquals(1, row1.size());
        KeyboardButton button1 = row1.get(0);
        assertEquals("\uD83D\uDD15reminder1", button1.getName());
        assertEquals("remind i1", button1.getCallback());

        List<KeyboardButton> row2 = keyboardButtonsList.get(1);
        assertEquals(1, row2.size());
        KeyboardButton button2 = row2.get(0);
        assertEquals("\uD83D\uDD14reminder2 w...", button2.getName());
        assertEquals("remind i2", button2.getCallback());

        List<KeyboardButton> row3 = keyboardButtonsList.get(2);
        assertEquals(1, row3.size());
        KeyboardButton button3 = row3.get(0);
        assertEquals("\uD83D\uDD14reminder3", button3.getName());
        assertEquals("remind i3", button3.getCallback());

        List<KeyboardButton> addRow = keyboardButtonsList.get(3);
        assertEquals(1, addRow.size());
        KeyboardButton addButton = addRow.get(0);
        assertEquals("\uD83C\uDD95${command.remind.button.add}", addButton.getName());
        assertEquals("remind add", addButton.getCallback());

        List<KeyboardButton> reloadRow = keyboardButtonsList.get(4);
        assertEquals(1, reloadRow.size());
        KeyboardButton reloadButton = reloadRow.get(0);
        assertEquals("\uD83D\uDD04${command.remind.button.reload}", reloadButton.getName());
        assertEquals("remind upd", reloadButton.getCallback());
    }

    private List<Reminder> getReminderWithRepeatabilityList() {
        return List.of(getNotifiedReminder(), getReminderWithLongName(), getReminderWithRepeatability());
    }

    private Reminder getNotifiedReminder() {
        return new Reminder()
                .setId(1L)
                .setUser(TestUtils.getUser())
                .setChat(TestUtils.getChat())
                .setDate(CURRENT_DATE.minusDays(1))
                .setTime(CURRENT_TIME.minusMinutes(1))
                .setText("reminder1")
                .setNotified(true);
    }

    private Reminder getReminderWithLongName() {
        return new Reminder()
                .setId(2L)
                .setUser(TestUtils.getUser())
                .setChat(TestUtils.getChat())
                .setDate(CURRENT_DATE)
                .setTime(CURRENT_TIME)
                .setText("reminder2 with very very very long text")
                .setNotified(false);
    }

    private Reminder getReminderWithRepeatability() {
        return new Reminder()
                .setId(3L)
                .setUser(TestUtils.getUser())
                .setChat(TestUtils.getChat())
                .setDate(CURRENT_DATE)
                .setTime(CURRENT_TIME.minusMinutes(1))
                .setText("reminder3")
                .setNotified(false)
                .setRepeatability("10,11,12,13,14,");
    }

}