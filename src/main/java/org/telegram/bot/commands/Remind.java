package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.Reminder;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.enums.ReminderRepeatability;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.DateUtils;
import org.telegram.bot.utils.TextUtils;

import javax.annotation.PostConstruct;
import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalAmount;
import java.util.Set;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.telegram.bot.utils.DateUtils.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class Remind implements Command {

    private static final ResponseSettings DEFAULT_RESPONSE_SETTINGS = new ResponseSettings()
            .setFormattingStyle(FormattingStyle.HTML)
            .setWebPagePreview(false);

    private static final String EMPTY_COMMAND = "remind";
    private static final String CALLBACK_COMMAND = EMPTY_COMMAND + " ";
    private static final String DELETE_COMMAND = "del";
    private static final String CALLBACK_DELETE_COMMAND = CALLBACK_COMMAND + DELETE_COMMAND;
    private static final String ADD_COMMAND = "add";
    private static final String CALLBACK_ADD_COMMAND = CALLBACK_COMMAND + ADD_COMMAND;
    private static final String UPDATE_COMMAND = "upd";
    private static final String CALLBACK_UPDATE_COMMAND = CALLBACK_COMMAND + UPDATE_COMMAND;
    private static final String SELECT_PAGE = "page";
    private static final String SET_REMINDER = "s";
    private static final String CALLBACK_SET_REMINDER = CALLBACK_COMMAND + SET_REMINDER;
    private static final String INFO_REMINDER = "i";
    private static final String CALLBACK_INFO_REMINDER = CALLBACK_COMMAND + INFO_REMINDER;
    private static final String CLOSE_REMINDER_MENU = "c";
    private static final String CALLBACK_CLOSE_REMINDER_MENU = CALLBACK_COMMAND + CLOSE_REMINDER_MENU;
    private static final String SET_DATE = "d";
    private static final String SET_TIME = "t";
    private static final String SET_REPEATABLE ="r";
    private static final String SET_NOTIFIED = "n";
    private static final int FIRST_PAGE = 0;

    private static final Pattern SET_FULL_DATE_PATTERN = Pattern.compile(SET_DATE + "(\\d{2})\\.(\\d{2})\\.(\\d{4})");
    private static final Pattern SET_SHORT_DATE_PATTERN = Pattern.compile(SET_DATE + "(\\d{2})\\.(\\d{2})");
    private static final Pattern SET_TIME_PATTERN = Pattern.compile(SET_TIME + "(\\d{2}):(\\d{2})");
    private static final Pattern FULL_DATE_PATTERN = Pattern.compile("(\\d{2})\\.(\\d{2})\\.(\\d{4})");
    private static final Pattern SHORT_DATE_PATTERN = Pattern.compile("(\\d{2})\\.(\\d{2})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{2}):(\\d{2})");
    private static final Pattern AFTER_HOURS_TIME_PATTERN = Pattern.compile("\\d+");
    private static final Pattern ID_PATTERN = Pattern.compile("s\\d+");
    private static final Pattern SET_ID_PATTERN = Pattern.compile(SET_REMINDER + "\\d+");
    private static final Pattern SET_NOTIFIED_PATTERN = Pattern.compile(SET_REMINDER + "\\d+" + SET_NOTIFIED);
    private static final Pattern SET_REPEATABLE_PATTERN = Pattern.compile(SET_REMINDER + "\\d+" + SET_REPEATABLE);
    private static final Pattern POSTPONE_PATTERN = Pattern.compile("P(?!$)(\\d+Y)?(\\d+M)?(\\d+W)?(\\d+D)?(T(?=\\d)(\\d+H)?(\\d+M)?(\\d+S)?)?");

    private Pattern afterMinutesPattern;
    private Pattern afterHoursPattern;
    private Pattern afterDaysPattern;

    private final Bot bot;
    private final ReminderService reminderService;
    private final CommandWaitingService commandWaitingService;
    private final UserCityService userCityService;
    private final SpeechService speechService;
    private final InternationalizationService internationalizationService;
    private final LanguageResolver languageResolver;
    private final BotStats botStats;

    private final Map<Set<String>, Function<ZoneId, LocalDate>> dateKeywords = new ConcurrentHashMap<>();
    private final Map<Set<String>, Supplier<LocalTime>> timeKeywords = new ConcurrentHashMap<>();

    @PostConstruct
    void postConstruct() {
        String template = "(%s)\\s+(\\d+)\\s+(%s)";
        String blockIn = String.join("|", internationalizationService.getAllTranslations("command.remind.in"));
        String minutesBlock = translationsSetToTemporaryUnitsPatternBlock(internationalizationService.getAllTranslations("command.remind.minutes"));
        String hoursBlock = translationsSetToTemporaryUnitsPatternBlock(internationalizationService.getAllTranslations("command.remind.hours"));
        String daysBlock = translationsSetToTemporaryUnitsPatternBlock(internationalizationService.getAllTranslations("command.remind.days"));

        afterMinutesPattern = Pattern.compile(String.format(template, blockIn, minutesBlock));
        afterHoursPattern = Pattern.compile(String.format(template, blockIn, hoursBlock));
        afterDaysPattern = Pattern.compile(String.format(template, blockIn, daysBlock));

        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.today")), LocalDate::now);
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.aftertomorrow")), zoneId -> LocalDate.now(zoneId).plusDays(2));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.tomorrow")), zoneId -> LocalDate.now(zoneId).plusDays(1));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.monday")), zoneId -> LocalDate.now(zoneId).with(TemporalAdjusters.next(DayOfWeek.MONDAY)));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.tuesday")), zoneId -> LocalDate.now(zoneId).with(TemporalAdjusters.next(DayOfWeek.TUESDAY)));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.wednesday")), zoneId -> LocalDate.now(zoneId).with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY)));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.thursday")), zoneId -> LocalDate.now(zoneId).with(TemporalAdjusters.next(DayOfWeek.THURSDAY)));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.friday")), zoneId -> LocalDate.now(zoneId).with(TemporalAdjusters.next(DayOfWeek.FRIDAY)));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.saturday")), zoneId -> LocalDate.now(zoneId).with(TemporalAdjusters.next(DayOfWeek.SATURDAY)));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.sunday")), zoneId -> LocalDate.now(zoneId).with(TemporalAdjusters.next(DayOfWeek.SUNDAY)));

        timeKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.timekeyword.morning")), () -> LocalTime.of(7, 0));
        timeKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.timekeyword.lunch")), () -> LocalTime.of(12, 0));
        timeKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.timekeyword.afternoon")), () -> LocalTime.of(15, 0));
        timeKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.timekeyword.evening")), () -> LocalTime.of(22, 0));
        timeKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.timekeyword.dinner")), () -> LocalTime.of(19, 0));
        timeKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.timekeyword.night")), () -> LocalTime.of(3, 0));
    }

    private String translationsSetToTemporaryUnitsPatternBlock(Set<String> translationsSet) {
        return translationsSet
                .stream()
                .map(translations -> translations.split("#"))
                .map(Arrays::asList)
                .flatMap(List::stream)
                .map(translation -> "(\\b" + translation + "\\b)")
                .collect(Collectors.joining("|"));
    }

    private Set<String> csvTranslationSetToTranslationSet(Set<String> translationsSet) {
        return translationsSet
                .stream()
                .map(translations -> translations.split("#"))
                .map(Arrays::asList)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        Chat chat = message.getChat();
        User user = message.getUser();
        String commandArgument;

        CommandWaiting commandWaiting = commandWaitingService.get(chat, user);

        if (commandWaiting != null) {
            String text = message.getText();
            if (text == null) {
                text = "";
            }
            commandArgument = TextUtils.cutCommandInText(commandWaiting.getTextMessage()) + text;
        } else {
            commandArgument = message.getCommandArgument();
        }

        if (commandWaiting != null && commandWaiting.getCommandName().equals(this.getClass().getSimpleName().toLowerCase(Locale.ROOT))) {
            commandWaitingService.remove(commandWaiting);
        }

        if (message.isCallback()) {
            if (TextUtils.isEmpty(commandArgument) || UPDATE_COMMAND.equals(commandArgument)) {
                return returnResponse(getReminderListWithKeyboard(message, chat, user, FIRST_PAGE, false));
            } else if (commandArgument.equals(ADD_COMMAND)) {
                return returnResponse(addReminderByCallback(message, chat, user));
            } else if (commandArgument.startsWith(INFO_REMINDER)) {
                return returnResponse(getReminderInfo(message, user, commandArgument));
            } else if (commandArgument.startsWith(SET_REMINDER)) {
                return returnResponse(setReminderByCallback(message, chat, user, commandArgument));
            } else if (commandArgument.startsWith(DELETE_COMMAND)) {
                return returnResponse(deleteReminderByCallback(message, chat, user, commandArgument));
            } else if (commandArgument.startsWith(SELECT_PAGE)) {
                return returnResponse(getReminderListWithKeyboard(message, chat, user, getPageNumberFromCommand(commandArgument), false));
            } else if (commandArgument.startsWith(CLOSE_REMINDER_MENU)) {
                return returnResponse(closeReminderMenu(message, user, commandArgument));
            }
        }

        if (commandArgument == null || commandArgument.equals(EMPTY_COMMAND)) {
            return returnResponse(getReminderListWithKeyboard(message, chat, user, FIRST_PAGE, true));
        } else if (commandArgument.startsWith(SET_REMINDER) && commandWaiting != null) {
            return returnResponse(manualReminderEdit(message, chat, user, commandArgument));
        } else {
            return returnResponse(addReminder(message, chat, user, commandArgument));
        }
    }

    private BotResponse getReminderInfo(Message message, User user, String command) {
        long reminderId;
        try {
            reminderId = Long.parseLong(command.substring(INFO_REMINDER.length()));
        } catch (NumberFormatException e) {
            botStats.incrementErrors(message, e, "${commad.remind.idparsingfail}");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        Reminder reminder = reminderService.get(reminderId);
        if (reminder == null) {
            return new DeleteResponse(message);
        }

        return new EditResponse(message)
                .setText(prepareReminderInfoText(reminder, languageResolver.getChatLanguageCode(message, user)))
                .setKeyboard(prepareKeyboardWithReminderInfo(reminder))
                .setResponseSettings(DEFAULT_RESPONSE_SETTINGS);
    }

    private BotResponse addReminder(Message message, Chat chat, User user, String command) {
        log.debug("Request to add new reminder: {}", command);

        ZoneId zoneIdOfUser = getZoneIdOfUser(chat, user);
        String reminderText;
        LocalDate reminderDate = null;
        LocalTime reminderTime = null;
        LocalDate dateNow = LocalDate.now(zoneIdOfUser);

        Pattern datePattern = FULL_DATE_PATTERN;
        Matcher matcher = datePattern.matcher(command);
        if (matcher.find()) {
            reminderDate = getDateFromText(command.substring(matcher.start(), matcher.end()));
        } else {
            datePattern = SHORT_DATE_PATTERN;
            matcher = datePattern.matcher(command);
            if (matcher.find()) {
                try {
                    reminderDate = getDateFromTextWithoutYear(command.substring(matcher.start(), matcher.end()), dateNow);
                } catch (Exception ignored) {
                    // date not found
                }
            }
        }

        matcher = TIME_PATTERN.matcher(command);
        if (matcher.find()) {
            reminderTime = getTimeFromText(command.substring(matcher.start(), matcher.end()));
        }

        if (reminderTime != null) {
            command = command.replaceFirst(TIME_PATTERN.pattern(), "").trim();
        } else {
            String afterMinutesPhrase = getAfterMinutesPhrase(command);
            if (afterMinutesPhrase != null) {
                Integer minutes = getValueFromAfterTimePhrase(afterMinutesPhrase);
                if (minutes != null) {
                    reminderTime = LocalTime.now(zoneIdOfUser).plusMinutes(minutes);
                    command = command.replaceFirst(afterMinutesPhrase, "").trim();
                }
            }

            if (reminderTime == null) {
                String afterHoursPhrase = getAfterHoursPhrase(command);
                if (afterHoursPhrase != null) {
                    Integer hours = getValueFromAfterTimePhrase(afterHoursPhrase);
                    if (hours != null) {
                        reminderTime = LocalTime.now(zoneIdOfUser).plusHours(hours);
                        command = command.replaceFirst(afterHoursPhrase, "").trim();
                    }
                }
            }

            if (reminderTime == null) {
                Pair<Set<String>, Supplier<LocalTime>> timeKeyword = getTimeKeywordFromText(command);
                if (timeKeyword != null) {
                    reminderTime = timeKeyword.getSecond().get();
                    command = cutKeyWordInText(command, timeKeyword.getFirst());
                }
            }

            if (reminderTime == null) {
                reminderTime = LocalTime.MIN;
            }
        }

        if (reminderDate != null) {
            command = command.replaceFirst(datePattern.pattern(), "").trim();
        } else {
            String afterDaysPhrase = getAfterDaysPhrase(command);
            if (afterDaysPhrase != null) {
                Integer days = getValueFromAfterTimePhrase(afterDaysPhrase);
                if (days != null) {
                    reminderDate = dateNow.plusDays(days);
                    command = command.replaceFirst(afterDaysPhrase, "").trim();
                }
            }

            if (reminderDate == null) {
                Pair<Set<String>, Function<ZoneId, LocalDate>> dateKeyword = getDateKeywordFromText(command);
                if (dateKeyword != null) {
                    reminderDate = dateKeyword.getSecond().apply(zoneIdOfUser);
                    command = cutKeyWordInText(command, dateKeyword.getFirst());
                }
            }

            if (reminderDate == null) {
                if (reminderTime == null || LocalTime.now(zoneIdOfUser).isAfter(reminderTime)) {
                    reminderDate = dateNow.plusDays(1);
                } else {
                    reminderDate = dateNow;
                }
            }
        }

        if (command.length() <= 1) {
            reminderText = "";
        } else {
            reminderText = command;
        }

        Reminder reminder = reminderService.save(new Reminder()
                .setChat(chat)
                .setUser(user)
                .setDate(reminderDate)
                .setTime(reminderTime)
                .setText(reminderText)
                .setNotified(false));

        return new TextResponse(message)
                .setText(prepareReminderInfoText(reminder, languageResolver.getChatLanguageCode(message, user)))
                .setKeyboard(prepareKeyboardWithReminderInfo(reminder))
                .setResponseSettings(DEFAULT_RESPONSE_SETTINGS);
    }

    private Pair<Set<String>, Supplier<LocalTime>> getTimeKeywordFromText(String text) {
        return timeKeywords
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().stream().anyMatch(text::contains))
                .findFirst()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                .orElse(null);
    }

    private Pair<Set<String>, Function<ZoneId, LocalDate>> getDateKeywordFromText(String text) {
        return dateKeywords
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().stream().anyMatch(text::contains))
                .findFirst()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                .orElse(null);
    }

    private String getAfterMinutesPhrase(String text) {
        if (text == null) {
            return null;
        }

        Matcher matcher = afterMinutesPattern.matcher(text);

        if (matcher.find()) {
            return text.substring(matcher.start(), matcher.end());
        }

        return null;
    }

    private String getAfterHoursPhrase(String text) {
        if (text == null) {
            return null;
        }

        Matcher matcher = afterHoursPattern.matcher(text);

        if (matcher.find()) {
            return text.substring(matcher.start(), matcher.end());
        }

        return null;
    }

    private String getAfterDaysPhrase(String text) {
        if (text == null) {
            return null;
        }

        Matcher matcher = afterDaysPattern.matcher(text);

        if (matcher.find()) {
            return text.substring(matcher.start(), matcher.end());
        }

        return null;
    }

    private Integer getValueFromAfterTimePhrase(String text) {
        Matcher matcher = AFTER_HOURS_TIME_PATTERN.matcher(text);

        if (matcher.find()) {
            int value = Integer.parseInt(text.substring(matcher.start(), matcher.end()));
            if (value > 0) {
                return value;
            }
        }

        return null;
    }

    private TextResponse manualReminderEdit(Message message, Chat chat, User user, String command) {
        command = command.replaceAll("\\s+","");

        Keyboard keyboard;

        Reminder reminder = null;
        String caption = "";
        Matcher matcher = ID_PATTERN.matcher(command);
        if (matcher.find()) {
            try {
                reminder = reminderService.get(
                        chat,
                        user,
                        Long.parseLong(command.substring(matcher.start() + SET_REMINDER.length(), matcher.end())));
            } catch (NumberFormatException ignored) {
                // reminder not found
            }

            if (reminder != null) {
                matcher = SET_FULL_DATE_PATTERN.matcher(command);
                if (matcher.find()) {
                    reminder.setDate(getDateFromText(command.substring(matcher.start() + 1, matcher.end())));
                } else {
                    matcher = SET_SHORT_DATE_PATTERN.matcher(command);
                    if (matcher.find()) {
                        ZoneId zoneIdOfUser = getZoneIdOfUser(chat, user);
                        reminder.setDate(getDateFromTextWithoutYear(command.substring(matcher.start() + 1, matcher.end()), LocalDate.now(zoneIdOfUser)));
                    } else {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                }

                if (command.indexOf(SET_TIME) < 1) {
                    commandWaitingService.add(chat, user, Remind.class, CALLBACK_COMMAND + SET_REMINDER + reminder.getId() +
                            SET_DATE + dateFormatter.format(reminder.getDate()) + SET_TIME);
                    keyboard = prepareKeyboardWithReminderInfo(prepareButtonRowsWithTimeSettings(reminder), reminder);
                    caption = "${command.remind.timeset}\n";

                    reminder.setTime(LocalTime.MIN);
                } else {
                    matcher = SET_TIME_PATTERN.matcher(command);
                    if (matcher.find()) {
                        reminder.setTime(getTimeFromText(command.substring(matcher.start() + 1, matcher.end())))
                                .setNotified(false);
                        keyboard = prepareKeyboardWithReminderInfo(reminder);
                    } else {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                }
            } else {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        reminderService.save(reminder);

        return new TextResponse(message)
                .setText(prepareReminderInfoText(reminder, languageResolver.getChatLanguageCode(message, user)) + "<b>" + caption + "</b>")
                .setKeyboard(keyboard)
                .setResponseSettings(DEFAULT_RESPONSE_SETTINGS);
    }

    private BotResponse setReminderByCallback(Message message, Chat chat, User user, String command) {
        Reminder reminder;

        Matcher reminderIdMatcher = SET_ID_PATTERN.matcher(command);
        if (reminderIdMatcher.find()) {
            long reminderId;
            try {
                reminderId = Long.parseLong(command.substring(reminderIdMatcher.start() + SET_REMINDER.length(), reminderIdMatcher.end()));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            reminder = reminderService.get(reminderId);
            if (reminder == null) {
                return new DeleteResponse(message);
            } else if (!reminder.getUser().getUserId().equals(user.getUserId())) {
                return null;
            }
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        List<List<KeyboardButton>> rowsWithButtons = new ArrayList<>();
        String caption = "";
        Matcher setNotifiedMatcher = SET_NOTIFIED_PATTERN.matcher(command);
        Matcher setRepeatableMatcher = SET_REPEATABLE_PATTERN.matcher(command);
        Matcher setFullDateMatcher = SET_FULL_DATE_PATTERN.matcher(command);
        Matcher setTimeMatcher = SET_TIME_PATTERN.matcher(command);

        if (setNotifiedMatcher.find()) {
            if (Boolean.TRUE.equals(reminder.getNotified())) {
                if (!TextUtils.isEmpty(reminder.getRepeatability())) {
                    LocalDateTime nextAlarmDateTime = reminderService.getNextAlarmDateTime(reminder);
                    reminder.setDate(nextAlarmDateTime.toLocalDate())
                            .setTime(nextAlarmDateTime.toLocalTime());
                }

                reminder.setNotified(false);
            } else {
                reminder.setNotified(true);
            }
            
            reminderService.save(reminder);
            return getReminderInfo(message, user, INFO_REMINDER + reminder.getId());
        } else if (setRepeatableMatcher.find()) {
            reminder.setRepeatability(command.substring(command.indexOf(SET_REPEATABLE) + 1));
            reminderService.save(reminder);
            caption = "${command.remind.repeatevery}: ";
            rowsWithButtons = prepareRepeatKeyboard(reminder);
        } else if (setFullDateMatcher.find()) {
            reminder.setDate(getDateFromText(command.substring(setFullDateMatcher.start() + 1, setFullDateMatcher.end())));
            if (setTimeMatcher.find()) {
                reminder.setTime(getTimeFromText(command.substring(setTimeMatcher.start() + 1, setTimeMatcher.end())))
                        .setNotified(false);
            } else {
                caption = "${command.remind.timeset}\n";
                rowsWithButtons = prepareButtonRowsWithTimeSettings(reminder);
                commandWaitingService.add(chat, user, Remind.class, CALLBACK_COMMAND + SET_REMINDER + reminder.getId() +
                        SET_DATE + dateFormatter.format(reminder.getDate()) + SET_TIME);
            }
        } else {
            ZoneId zoneIdOfUser = getZoneIdOfUser(chat, user);

            Matcher setPostponeMatcher = POSTPONE_PATTERN.matcher(command);
            if (setPostponeMatcher.find()) {
                TemporalAmount temporalAmount;
                String rawValue = command.substring(setPostponeMatcher.start(), setPostponeMatcher.end());
                try {
                    temporalAmount = Duration.parse(rawValue);
                } catch (Exception e) {
                    temporalAmount = Period.parse(rawValue);
                }

                LocalDateTime reminderDateTime = LocalDateTime.now(zoneIdOfUser).plus(temporalAmount);

                if (TextUtils.isEmpty(reminder.getRepeatability())) {
                    reminder.setDate(reminderDateTime.toLocalDate())
                            .setTime(reminderDateTime.toLocalTime())
                            .setNotified(false);
                } else {
                    reminder = new Reminder()
                            .setUser(user)
                            .setChat(chat)
                            .setDate(reminderDateTime.toLocalDate())
                            .setTime(reminderDateTime.toLocalTime())
                            .setText("(${command.remind.copy}) " + reminder.getText())
                            .setNotified(false);
                }
            } else {
                caption = "${command.remind.dateset}\n";
                rowsWithButtons = prepareButtonRowsWithDateSetting(reminder, zoneIdOfUser);
                commandWaitingService.add(chat, user, Remind.class, CALLBACK_COMMAND + SET_REMINDER + reminder.getId() +
                        SET_DATE);
            }
        }

        reminderService.save(reminder);

        String languageCode = languageResolver.getChatLanguageCode(message, user);
        Keyboard keyboard = prepareKeyboardWithReminderInfo(rowsWithButtons, reminder);

        return new EditResponse(message)
                .setText(prepareReminderInfoText(reminder, languageCode) + "<b>" + caption + "</b>")
                .setKeyboard(keyboard)
                .setResponseSettings(DEFAULT_RESPONSE_SETTINGS);
    }

    private LocalDate getDateFromTextWithoutYear(String text, LocalDate dateNow) {
        LocalDate dateFromText = getDateFromText(text + "." + dateNow.getYear());

        if (dateFromText.isBefore(dateNow)) {
            return dateFromText.plusYears(1);
        }

        return dateFromText;
    }

    private LocalDate getDateFromText(String text) {
        try {
            return LocalDate.parse(text, dateFormatter);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private LocalTime getTimeFromText(String text) {
        try {
            return LocalTime.parse(text, timeShortFormatter);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private String prepareReminderInfoText(Reminder reminder, String lang) {
        ZoneId zoneId = userCityService.getZoneIdOfUser(reminder.getChat(), reminder.getUser());
        if (zoneId == null) {
            zoneId = ZoneId.systemDefault();
        }

        LocalDateTime dateTimeNow = LocalDateTime.now(zoneId);
        LocalDate reminderDate = reminder.getDate();
        LocalTime reminderTime = reminder.getTime();
        LocalDateTime reminderDateTime = reminderDate.atTime(reminderTime);

        String repeatability;
        String reminderRepeatability = reminder.getRepeatability();
        if (TextUtils.isEmpty(reminderRepeatability)) {
            repeatability = "${command.remind.withoutrepeat}";
        } else {
            repeatability = getRepeatabilityInfo(reminderRepeatability);
        }

        String deltaDates = deltaDatesToString(reminderDateTime, dateTimeNow);
        String leftToRun;
        boolean notified = Boolean.TRUE.equals(reminder.getNotified());
        if (dateTimeNow.isAfter(reminderDateTime) && notified) {
            leftToRun = "${command.remind.worked}: <b>" + deltaDates + "</b>${command.remind.ago}\n";
        } else if (dateTimeNow.isAfter(reminderDateTime) && !notified) {
            leftToRun = "${command.remind.beforetriggering}: <b> ${command.remind.almostthere} </b>\n";
        } else if (dateTimeNow.isBefore(reminderDateTime) && notified) {
            leftToRun = "${command.remind.disabled}\n";
        } else {
            leftToRun = "${command.remind.beforetriggering}: <b>" + deltaDates + "</b>\n";
        }

        return "<b>${command.remind.caption}</b>\n" +
                reminder.getText() + "\n<i>" +
                formatDate(reminderDate) + " " + formatTime(reminderTime) + " (" + getDayOfWeek(reminderDate, lang) + " )</i>\n" +
                "Повтор: <b>" + repeatability + "</b>\n" +
                leftToRun;
    }

    private String getRepeatabilityInfo(String reminderRepeatability) {
        if (TextUtils.isEmpty(reminderRepeatability)) {
            return "";
        }
        return Arrays.stream(reminderRepeatability.split("\\s*,\\s*"))
                .map(Integer::valueOf)
                .map(ordinal -> ReminderRepeatability.values()[ordinal])
                .map(ReminderRepeatability::getCaption)
                .collect(Collectors.joining(", "));
    }

    private List<List<KeyboardButton>> prepareButtonRowsWithDateSetting(Reminder reminder, ZoneId zoneId) {
        Long reminderId = reminder.getId();
        LocalDate reminderDate = reminder.getDate();
        LocalDate dateNow = LocalDate.now(zoneId);
        List<List<KeyboardButton>> rows = new ArrayList<>();
        String startOfCallbackCommand = CALLBACK_SET_REMINDER + reminderId + SET_DATE;

        if (reminderDate != null) {
            String formattedDate = dateFormatter.format(reminderDate);
            rows.add(List.of(new KeyboardButton()
                    .setName("${command.remind.leave} " + formattedDate)
                    .setCallback(startOfCallbackCommand + formattedDate)));
        }

        rows.add(List.of(new KeyboardButton()
                .setName("${command.remind.today}")
                .setCallback(startOfCallbackCommand + dateFormatter.format(dateNow))));

        rows.add(List.of(new KeyboardButton()
                .setName("${command.remind.tomorrow}")
                .setCallback(startOfCallbackCommand + dateFormatter.format(dateNow.plusDays(1)))));

        rows.add(List.of(new KeyboardButton()
                .setName("${command.remind.aftertomorrow}")
                .setCallback(startOfCallbackCommand + dateFormatter.format(dateNow.plusDays(2)))));

        rows.add(List.of(new KeyboardButton()
                .setName("${command.remind.onsaturday}")
                .setCallback(startOfCallbackCommand + dateFormatter.format(dateNow.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))))));

        rows.add(List.of(new KeyboardButton()
                .setName("${command.remind.onsunday}")
                .setCallback(startOfCallbackCommand + dateFormatter.format(dateNow.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))))));

        return rows;
    }

    private List<List<KeyboardButton>> prepareButtonRowsWithTimeSettings(Reminder reminder) {
        Long reminderId = reminder.getId();
        List<List<KeyboardButton>> rows = new ArrayList<>();

        String startOfCallbackCommand = CALLBACK_SET_REMINDER + reminderId +
                    SET_DATE + dateFormatter.format(reminder.getDate()) + SET_TIME;

        if (reminder.getTime() != null) {
            String formattedTime = timeShortFormatter.format(reminder.getTime());
            rows.add(List.of(new KeyboardButton()
                    .setName("${command.remind.leave} " + formattedTime)
                    .setCallback(startOfCallbackCommand + formattedTime)));
        }

        String morningTime = "07:00";
        rows.add(List.of(new KeyboardButton()
                .setName("${command.remind.morning} " + morningTime)
                .setCallback(startOfCallbackCommand + morningTime)));

        String lunchTime = "13:00";
        rows.add(List.of(new KeyboardButton()
                .setName("${command.remind.lunch} " + lunchTime)
                .setCallback(startOfCallbackCommand + lunchTime)));

        String dinnerTime = "18:00";
        rows.add(List.of(new KeyboardButton()
                .setName("${command.remind.dinner} " + dinnerTime)
                .setCallback(startOfCallbackCommand + dinnerTime)));

        String eveningTime = "20:00";
        rows.add(List.of(new KeyboardButton()
                .setName("${command.remind.evening} " + eveningTime)
                .setCallback(startOfCallbackCommand + eveningTime)));

        String nightTime = "03:00";
        rows.add(List.of(new KeyboardButton()
                .setName("${command.remind.night} " + nightTime)
                .setCallback(startOfCallbackCommand + nightTime)));

        return rows;
    }

    private static List<List<KeyboardButton>> prepareRepeatKeyboard(Reminder reminder) {
        List<List<KeyboardButton>> rows = new ArrayList<>();

        rows.add(
                Stream.of(
                        ReminderRepeatability.MINUTES1,
                        ReminderRepeatability.MINUTES5,
                        ReminderRepeatability.MINUTES10,
                        ReminderRepeatability.MINUTES15,
                        ReminderRepeatability.MINUTES30)
                    .map(reminderRepeatability -> generateRepeatButton(reminderRepeatability, reminder))
                    .toList());

        rows.add(
                Stream.of(
                        ReminderRepeatability.HOURS1,
                        ReminderRepeatability.HOURS2,
                        ReminderRepeatability.HOURS3,
                        ReminderRepeatability.HOURS6,
                        ReminderRepeatability.HOURS12)
                .map(reminderRepeatability -> generateRepeatButton(reminderRepeatability, reminder))
                .toList());

        rows.add(
                Stream.of(
                        ReminderRepeatability.MONDAY,
                        ReminderRepeatability.TUESDAY,
                        ReminderRepeatability.WEDNESDAY,
                        ReminderRepeatability.THURSDAY,
                        ReminderRepeatability.FRIDAY,
                        ReminderRepeatability.SATURDAY,
                        ReminderRepeatability.SUNDAY)
                .map(reminderRepeatability -> generateRepeatButton(reminderRepeatability, reminder))
                .toList());

        rows.add(
                Stream.of(
                        ReminderRepeatability.DAY,
                        ReminderRepeatability.WEEK,
                        ReminderRepeatability.MONTH,
                        ReminderRepeatability.YEAR)
                .map(reminderRepeatability -> generateRepeatButton(reminderRepeatability, reminder))
                .toList());

        return rows;
    }

    private static KeyboardButton generateRepeatButton(ReminderRepeatability reminderRepeatability, Reminder reminder) {
        String callbackData;
        String currentRepeatability = reminder.getRepeatability();
        String ordinal = String.valueOf(reminderRepeatability.ordinal());
        if (TextUtils.isEmpty(currentRepeatability)) {
            callbackData = CALLBACK_SET_REMINDER + reminder.getId() + SET_REPEATABLE + currentRepeatability + ordinal;
        } else if (currentRepeatability.contains(ordinal)) {
            callbackData = CALLBACK_SET_REMINDER + reminder.getId() + SET_REPEATABLE +
                    currentRepeatability.replaceFirst(ordinal + ",*", "");
        } else {
            callbackData = CALLBACK_SET_REMINDER + reminder.getId() + SET_REPEATABLE + currentRepeatability + "," + ordinal;
        }

        if (callbackData.endsWith(",")) {
            callbackData = callbackData.substring(0, callbackData.length() - 1);
        }

        KeyboardButton setRepeatButton = new KeyboardButton();
        setRepeatButton.setName(reminderRepeatability.getCaption());
        setRepeatButton.setCallback(callbackData);

        return setRepeatButton;
    }

    private Keyboard prepareKeyboardWithReminderInfo(Reminder reminder) {
        return prepareKeyboardWithReminderInfo(new ArrayList<>(), reminder);
    }

    private Keyboard prepareKeyboardWithReminderInfo(List<List<KeyboardButton>> rows, Reminder reminder) {
        addMainRows(rows, reminder, false);
        return new Keyboard(rows);
    }

    private EditResponse addReminderByCallback(Message message, Chat chat, User user) {
        log.debug("Request to add reminder after callback");
        commandWaitingService.add(chat, user, Remind.class, CALLBACK_COMMAND);
        return new EditResponse(message)
                .setText("\n${command.remind.commandwaitingstart}");
    }

    private BotResponse deleteReminderByCallback(Message message, Chat chat, User user, String command) {
        log.debug("Request to delete reminder");

        if (command.equals(DELETE_COMMAND)) {
            return getReminderListWithKeyboard(message, chat, user, FIRST_PAGE, false);
        }

        boolean deleteReminderMessage = false;
        if (command.endsWith(CLOSE_REMINDER_MENU)) {
            command = command.substring(0, command.length() - 1);
            deleteReminderMessage = true;
        }

        long reminderId;
        try {
            reminderId = Long.parseLong(command.substring(DELETE_COMMAND.length()));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        Reminder reminder = reminderService.get(reminderId);
        if (reminder == null) {
            return new DeleteResponse(message);
        } else if (!reminder.getUser().getUserId().equals(user.getUserId())) {
            return null;
        }

        reminderService.remove(reminder);

        if (deleteReminderMessage) {
            return new DeleteResponse(message);
        }

        return getReminderListWithKeyboard(message, chat, user, FIRST_PAGE, false);
    }

    private BotResponse getReminderListWithKeyboard(Message message, Chat chat, User user, int page, boolean newMessage) {
        log.debug("Request to list all reminders for chat {} and user {}, page: {}", chat.getChatId(), user.getUserId(), page);
        Page<Reminder> reminderList = reminderService.getByChatAndUser(chat, user, page);

        String languageCode = languageResolver.getChatLanguageCode(message, user);
        String caption = TextUtils.getLinkToUser(user, true) + "<b> ${command.remind.yourreminders}:</b>\n" + buildTextReminderList(reminderList.toList(), languageCode);
        Keyboard keyboard = prepareKeyboardWithRemindersForSetting(reminderList, page, languageCode);

        if (newMessage) {
            return new TextResponse(message)
                    .setText(caption)
                    .setKeyboard(keyboard)
                    .setResponseSettings(DEFAULT_RESPONSE_SETTINGS);
        }

        return new EditResponse(message)
                .setText(caption)
                .setKeyboard(keyboard)
                .setResponseSettings(DEFAULT_RESPONSE_SETTINGS);
    }

    private String buildTextReminderList(List<Reminder> reminderList, String lang) {
        return reminderList
                .stream()
                .map(reminder -> {
                    LocalDateTime reminderDateTime = reminder.getDate().atTime(reminder.getTime());
                    String additionally = TextUtils.isEmpty(reminder.getRepeatability()) ? "" : Emoji.CALENDAR.getSymbol();
                    additionally = " " + getConditionalEmoji(reminder) + additionally;

                    return DateUtils.formatShortDateTime(reminderDateTime) + " (" + getDayOfWeek(reminderDateTime, lang) + " )" +
                            " — " + reminder.getText() + additionally;
                })
                .collect(Collectors.joining("\n"));
    }

    private DeleteResponse closeReminderMenu(Message message, User user, String command) {
        log.debug("Request to close reminder menu by messageId {}", message.getMessageId());

        String userId = command.substring(CLOSE_REMINDER_MENU.length());
        if (user.getUserId().toString().equals(userId)) {
            return new DeleteResponse(message);
        }

        return null;
    }

    private Keyboard prepareKeyboardWithRemindersForSetting(Page<Reminder> reminderList, int page, String lang) {
        final int maxButtonTextLength = 14;

        List<List<KeyboardButton>> rows = reminderList.stream().map(reminder -> {
            String reminderText = internationalizationService.internationalize(reminder.getText(), lang);
            if (reminderText.length() > 14) {
                reminderText = reminderText.substring(0, maxButtonTextLength - 3) + "...";
            }

            reminderText = getConditionalEmoji(reminder) + reminderText;

            return List.of(new KeyboardButton()
                    .setName(reminderText)
                    .setCallback(CALLBACK_INFO_REMINDER + reminder.getId()));
        }).collect(Collectors.toList());

        addingMainRows(rows, page, reminderList.getTotalPages());

        return new Keyboard(rows);
    }

    private String getConditionalEmoji(Reminder reminder) {
        if (Boolean.TRUE.equals(reminder.getNotified())) {
            return Emoji.NO_BELL.getSymbol();
        } else {
            return Emoji.BELL.getSymbol();
        }
    }

    private void addingMainRows(List<List<KeyboardButton>> rows, int page, int totalPages) {
        List<KeyboardButton> pagesRow = new ArrayList<>();

        if (page > 0) {
            pagesRow.add(new KeyboardButton()
                    .setName(Emoji.LEFT_ARROW.getSymbol() + "${command.remind.button.back}")
                    .setCallback(CALLBACK_COMMAND + SELECT_PAGE + (page - 1)));
        }

        if (page + 1 < totalPages) {
            pagesRow.add(new KeyboardButton()
                    .setName("${command.remind.button.forward}" + Emoji.RIGHT_ARROW.getSymbol())
                    .setCallback(CALLBACK_COMMAND + SELECT_PAGE + (page + 1)));
        }

        rows.add(pagesRow);

        rows.add(List.of(new KeyboardButton()
                .setName(Emoji.NEW.getSymbol() + "${command.remind.button.add}")
                .setCallback(CALLBACK_ADD_COMMAND)));
        rows.add(List.of(new KeyboardButton()
                .setName(Emoji.UPDATE.getSymbol() + "${command.remind.button.reload}")
                .setCallback(CALLBACK_UPDATE_COMMAND)));
    }

    private int getPageNumberFromCommand(String command) {
        try {
            return Integer.parseInt(command.substring(SELECT_PAGE.length()));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

    public static String prepareTextOfReminder(Reminder reminder) {
        return "<b>${command.remind.caption}</b>\n" +
                reminder.getText() + "\n<i>" +
                formatDate(reminder.getDate()) + " " + formatTime(reminder.getTime()) + "</i>\n" +
                "${command.remind.postponeuntil}:\n";
    }

    public static Keyboard preparePostponeKeyboard(Reminder reminder, ZoneId zoneId, Locale locale) {
        Long reminderId = reminder.getId();
        List<List<KeyboardButton>> rows = new ArrayList<>();

        rows.add(Stream.of(1, 5, 10, 15, 30)
                .map(minutes -> generatePostponeButton(reminderId, minutes + " ${command.remind.m}.", Duration.of(minutes, MINUTES)))
                .toList());

        rows.add(Stream.of(1, 2, 3, 6, 12)
                .map(hours -> generatePostponeButton(reminderId, hours + " ${command.remind.h}.", Duration.of(hours, HOURS)))
                .collect(Collectors.toList()));

        LocalDate dateNow = LocalDate.now(zoneId);
        rows.add(Arrays.stream(DayOfWeek.values())
                .map(dayOfWeek -> generatePostponeButton(
                        reminderId,
                        dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                        Period.between(dateNow, dateNow.with(TemporalAdjusters.next(dayOfWeek)))))
                .toList());

        rows.add(
                List.of(
                        generatePostponeButton(reminderId, "${command.remind.day}", Period.ofDays(1)),
                        generatePostponeButton(reminderId, "${command.remind.week}", Period.ofWeeks(1)),
                        generatePostponeButton(reminderId, "${command.remind.month}", Period.ofMonths(1)),
                        generatePostponeButton(reminderId, "${command.remind.year}", Period.ofYears(1))));

        addMainRows(rows, reminder, true);

        return new Keyboard(rows);
    }

    private static KeyboardButton generatePostponeButton(Long reminderId, String text, TemporalAmount amount) {
        return new KeyboardButton()
                .setName(text)
                .setCallback(CALLBACK_SET_REMINDER + reminderId + amount);
    }

    private String cutKeyWordInText(String text, Set<String> keywords) {
        String foundKeyword = keywords
                .stream()
                .filter(text::contains)
                .findFirst()
                .orElseThrow(() -> new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)));

        return text.replaceFirst(foundKeyword, "");
    }

    private static void addMainRows(List<List<KeyboardButton>> rows, Reminder reminder, boolean fromPostponeMenu) {
        Long reminderId = reminder.getId();

        String deleteCommand = CALLBACK_DELETE_COMMAND + reminderId;
        if (fromPostponeMenu) {
            deleteCommand = deleteCommand + CLOSE_REMINDER_MENU;
        }

        String notifiedCaption;
        if (Boolean.TRUE.equals(reminder.getNotified())) {
            notifiedCaption = Emoji.PLAY_BUTTON.getSymbol();
        } else {
            notifiedCaption = Emoji.STOP_BUTTON.getSymbol();
        }

        String repeatability = reminder.getRepeatability() == null ? "" : reminder.getRepeatability();

        rows.add(
                List.of(
                        new KeyboardButton()
                                .setName(Emoji.SETTINGS.getSymbol())
                                .setCallback(CALLBACK_SET_REMINDER + reminderId),
                        new KeyboardButton()
                                .setName(Emoji.DELETE.getSymbol())
                                .setCallback(deleteCommand),
                        new KeyboardButton()
                                .setName(notifiedCaption)
                                .setCallback(CALLBACK_SET_REMINDER + reminderId + SET_NOTIFIED),
                        new KeyboardButton()
                                .setName(Emoji.CALENDAR.getSymbol())
                                .setCallback(CALLBACK_SET_REMINDER + reminderId + SET_REPEATABLE + repeatability),
                        new KeyboardButton()
                                .setName(Emoji.UPDATE.getSymbol())
                                .setCallback(CALLBACK_INFO_REMINDER + reminderId),
                        new KeyboardButton()
                                .setName(Emoji.CHECK_MARK_BUTTON.getSymbol())
                                .setCallback(CALLBACK_CLOSE_REMINDER_MENU + reminder.getUser().getUserId())));

        if (!fromPostponeMenu) {
            rows.add(List.of(new KeyboardButton()
                    .setName(Emoji.LEFT_ARROW.getSymbol() + "${command.remind.button.back}")
                    .setCallback(CALLBACK_COMMAND)));
        }
    }

    private ZoneId getZoneIdOfUser(Chat chat, User user) {
        ZoneId zoneIdOfUser = userCityService.getZoneIdOfUser(chat, user);
        if (zoneIdOfUser == null) {
            zoneIdOfUser = ZoneId.systemDefault();
        }

        return zoneIdOfUser;
    }

}
