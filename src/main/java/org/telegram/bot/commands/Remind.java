package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.ReminderRepeatability;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.DateUtils;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import javax.annotation.PostConstruct;
import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.*;
import static org.telegram.bot.utils.DateUtils.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class Remind implements Command<PartialBotApiMethod<?>> {

    private static final String CALLBACK_COMMAND = "remind ";
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
    private final UserService userService;
    private final UserCityService userCityService;
    private final SpeechService speechService;
    private final InternationalizationService internationalizationService;
    private final LanguageResolver languageResolver;
    private final BotStats botStats;

    private final Map<Set<String>, Supplier<LocalDate>> dateKeywords = new ConcurrentHashMap<>();
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
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.aftertomorrow")), () -> LocalDate.now().plusDays(2));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.tomorrow")), () -> LocalDate.now().plusDays(1));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.monday")), () -> LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.tuesday")), () -> LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY)));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.wednesday")), () -> LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY)));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.thursday")), () -> LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.THURSDAY)));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.friday")), () -> LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.FRIDAY)));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.saturday")), () -> LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY)));
        dateKeywords.put(csvTranslationSetToTranslationSet(internationalizationService.getAllTranslations("command.remind.datekeyword.sunday")), () -> LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY)));

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
    public PartialBotApiMethod<?> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        Chat chat = new Chat().setChatId(message.getChatId());
        String textMessage;
        boolean callback = false;
        String emptyCommand = "remind";

        CommandWaiting commandWaiting = commandWaitingService.get(chat, new User().setUserId(message.getFrom().getId()));

        if (commandWaiting != null) {
            String text = message.getText();
            if (text == null) {
                text = "";
            }
            textMessage = cutCommandInText(commandWaiting.getTextMessage()) + text;
        } else {
            if (update.hasCallbackQuery()) {
                commandWaiting = commandWaitingService.get(chat, new User().setUserId(update.getCallbackQuery().getFrom().getId()));
                CallbackQuery callbackQuery = update.getCallbackQuery();
                textMessage = cutCommandInText(callbackQuery.getData());
                callback = true;
            } else {
                textMessage = cutCommandInText(message.getText());
            }
        }

        if (commandWaiting != null && commandWaiting.getCommandName().equals(this.getClass().getSimpleName().toLowerCase(Locale.ROOT))) {
            commandWaitingService.remove(commandWaiting);
        }

        if (callback) {
            User user = userService.get(update.getCallbackQuery().getFrom().getId());

            if (StringUtils.isEmpty(textMessage) || UPDATE_COMMAND.equals(textMessage)) {
                return getReminderListWithKeyboard(message, chat, user, FIRST_PAGE, false);
            } else if (textMessage.equals(ADD_COMMAND)) {
                return addReminderByCallback(message, chat, user);
            } else if (textMessage.startsWith(INFO_REMINDER)) {
                return getReminderInfo(message, chat, user, textMessage);
            } else if (textMessage.startsWith(SET_REMINDER)) {
                return setReminderByCallback(message, chat, user, textMessage);
            } else if (textMessage.startsWith(DELETE_COMMAND)) {
                return deleteReminderByCallback(message, chat, user, textMessage);
            } else if (textMessage.startsWith(SELECT_PAGE)) {
                return getReminderListWithKeyboard(message, chat, user, getPageNumberFromCommand(textMessage), false);
            } else if (textMessage.startsWith(CLOSE_REMINDER_MENU)) {
                return closeReminderMenu(message, chat, user, textMessage);
            }
        }

        User user = userService.get(message.getFrom().getId());
        if (textMessage == null || textMessage.equals(emptyCommand)) {
            return getReminderListWithKeyboard(message, chat, user, FIRST_PAGE, true);
        } else if (textMessage.startsWith(SET_REMINDER) && commandWaiting != null) {
            return manualReminderEdit(message, chat, user, textMessage);
        } else {
            return addReminder(message, chat, user, textMessage);
        }
    }

    private PartialBotApiMethod<?> getReminderInfo(Message message, Chat chat, User user, String command) {
        long reminderId;
        try {
            reminderId = Long.parseLong(command.substring(INFO_REMINDER.length()));
        } catch (NumberFormatException e) {
            botStats.incrementErrors(message, e, "${commad.remind.idparsingfail}");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        Reminder reminder = reminderService.get(chat, user, reminderId);
        if (reminder == null) {
            return generateDeleteMessage(chat, message);
        }

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(message.getChatId().toString());
        editMessage.setMessageId(message.getMessageId());
        editMessage.enableHtml(true);
        editMessage.disableWebPagePreview();
        editMessage.setText(prepareReminderInfoText(reminder, languageResolver.getChatLanguageCode(message, user)));
        editMessage.setReplyMarkup(prepareKeyboardWithReminderInfo(reminder));

        return editMessage;
    }

    private PartialBotApiMethod<?> addReminder(Message message, Chat chat, User user, String command) {
        log.debug("Request to add new reminder: {}", command);

        String reminderText;
        LocalDate reminderDate = null;
        LocalTime reminderTime = null;
        LocalDate dateNow = LocalDate.now();

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
                    reminderTime = LocalTime.now().plusMinutes(minutes);
                    command = command.replaceFirst(afterMinutesPhrase, "").trim();
                }
            }

            if (reminderTime == null) {
                String afterHoursPhrase = getAfterHoursPhrase(command);
                if (afterHoursPhrase != null) {
                    Integer hours = getValueFromAfterTimePhrase(afterHoursPhrase);
                    if (hours != null) {
                        reminderTime = LocalTime.now().plusHours(hours);
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
                Pair<Set<String>, Supplier<LocalDate>> dateKeyword = getDateKeywordFromText(command);
                if (dateKeyword != null) {
                    reminderDate = dateKeyword.getSecond().get();
                    command = cutKeyWordInText(command, dateKeyword.getFirst());
                }
            }

            if (reminderDate == null) {
                if (reminderTime == null || LocalTime.now().isAfter(reminderTime)) {
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

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setText(prepareReminderInfoText(reminder, languageResolver.getChatLanguageCode(message, user)));
        sendMessage.setReplyMarkup(prepareKeyboardWithReminderInfo(reminder));

        return sendMessage;
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

    private Pair<Set<String>, Supplier<LocalDate>> getDateKeywordFromText(String text) {
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

    private SendMessage manualReminderEdit(Message message, Chat chat, User user, String command) {
        command = command.replaceAll("\\s+","");

        InlineKeyboardMarkup keyboard;

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
                        reminder.setDate(getDateFromTextWithoutYear(command.substring(matcher.start() + 1, matcher.end()), LocalDate.now()));
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

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setText(prepareReminderInfoText(reminder, languageResolver.getChatLanguageCode(message, user)) + "<b>" + caption + "</b>");
        sendMessage.setReplyMarkup(keyboard);

        return sendMessage;
    }

    private PartialBotApiMethod<?> setReminderByCallback(Message message, Chat chat, User user, String command) {
        Reminder reminder;

        Matcher reminderIdMatcher = SET_ID_PATTERN.matcher(command);
        if (reminderIdMatcher.find()) {
            long reminderId;
            try {
                reminderId = Long.parseLong(command.substring(reminderIdMatcher.start() + SET_REMINDER.length(), reminderIdMatcher.end()));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            reminder = reminderService.get(chat, user, reminderId);
            if (reminder == null) {
                return generateDeleteMessage(chat, message);
            }
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        List<List<InlineKeyboardButton>> rowsWithButtons = new ArrayList<>();
        String caption = "";
        Matcher setNotifiedMatcher = SET_NOTIFIED_PATTERN.matcher(command);
        Matcher setRepeatableMatcher = SET_REPEATABLE_PATTERN.matcher(command);
        Matcher setFullDateMatcher = SET_FULL_DATE_PATTERN.matcher(command);
        Matcher setTimeMatcher = SET_TIME_PATTERN.matcher(command);

        if (setNotifiedMatcher.find()) {
            if (Boolean.TRUE.equals(reminder.getNotified())) {
                if (!StringUtils.isEmpty(reminder.getRepeatability())) {
                    LocalDateTime nextAlarmDateTime = reminderService.getNextAlarmDateTime(reminder);
                    reminder.setDate(nextAlarmDateTime.toLocalDate())
                            .setTime(nextAlarmDateTime.toLocalTime());
                }

                reminder.setNotified(false);
            } else {
                reminder.setNotified(true);
            }
            
            reminderService.save(reminder);
            return getReminderInfo(message, chat, user, INFO_REMINDER + reminder.getId());
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
            Matcher setPostponeMatcher = POSTPONE_PATTERN.matcher(command);
            if (setPostponeMatcher.find()) {
                TemporalAmount temporalAmount;
                String rawValue = command.substring(setPostponeMatcher.start(), setPostponeMatcher.end());
                try {
                    temporalAmount = Duration.parse(rawValue);
                } catch (Exception e) {
                    temporalAmount = Period.parse(rawValue);
                }

                LocalDateTime reminderDateTime = LocalDateTime.now().plus(temporalAmount);

                if (StringUtils.isEmpty(reminder.getRepeatability())) {
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
                rowsWithButtons = prepareButtonRowsWithDateSetting(reminder);
                commandWaitingService.add(chat, user, Remind.class, CALLBACK_COMMAND + SET_REMINDER + reminder.getId() +
                        SET_DATE);
            }
        }

        reminderService.save(reminder);

        String languageCode = languageResolver.getChatLanguageCode(message, user);
        InlineKeyboardMarkup keyboard = prepareKeyboardWithReminderInfo(rowsWithButtons, reminder);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.disableWebPagePreview();
        editMessageText.setText(prepareReminderInfoText(reminder, languageCode) + "<b>" + caption + "</b>");
        editMessageText.setReplyMarkup(keyboard);

        return editMessageText;
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

        LocalDateTime dateTimeNow = ZonedDateTime.now(zoneId).toLocalDateTime();
        LocalDate reminderDate = reminder.getDate();
        LocalTime reminderTime = reminder.getTime();
        LocalDateTime reminderDateTime = reminderDate.atTime(reminderTime);

        String repeatability;
        String reminderRepeatability = reminder.getRepeatability();
        if (StringUtils.isEmpty(reminderRepeatability)) {
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
        if (StringUtils.isEmpty(reminderRepeatability)) {
            return "";
        }
        return Arrays.stream(reminderRepeatability.split("\\s*,\\s*"))
                .map(Integer::valueOf)
                .map(ordinal -> ReminderRepeatability.values()[ordinal])
                .map(ReminderRepeatability::getCaption)
                .collect(Collectors.joining(", "));
    }

    private List<List<InlineKeyboardButton>> prepareButtonRowsWithDateSetting(Reminder reminder) {
        Long reminderId = reminder.getId();
        LocalDate reminderDate = reminder.getDate();
        LocalDate dateNow = LocalDate.now();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        String startOfCallbackCommand = CALLBACK_SET_REMINDER + reminderId + SET_DATE;

        if (reminderDate != null) {
            String formattedDate = dateFormatter.format(reminderDate);

            InlineKeyboardButton storedDateButton = new InlineKeyboardButton();
            storedDateButton.setText("${command.remind.leave} " + formattedDate);
            storedDateButton.setCallbackData(startOfCallbackCommand + formattedDate);
            rows.add(List.of(storedDateButton));
        }

        InlineKeyboardButton todayButton = new InlineKeyboardButton();
        todayButton.setText("${command.remind.today}");
        todayButton.setCallbackData(startOfCallbackCommand + dateFormatter.format(dateNow));
        rows.add(List.of(todayButton));

        InlineKeyboardButton tomorrowButton = new InlineKeyboardButton();
        tomorrowButton.setText("${command.remind.tomorrow}");
        tomorrowButton.setCallbackData(startOfCallbackCommand + dateFormatter.format(dateNow.plusDays(1)));
        rows.add(List.of(tomorrowButton));

        InlineKeyboardButton afterTomorrowButton = new InlineKeyboardButton();
        afterTomorrowButton.setText("${command.remind.aftertomorrow}");
        afterTomorrowButton.setCallbackData(startOfCallbackCommand + dateFormatter.format(dateNow.plusDays(2)));
        rows.add(List.of(afterTomorrowButton));

        InlineKeyboardButton saturdayButton = new InlineKeyboardButton();
        saturdayButton.setText("${command.remind.onsaturday}");
        saturdayButton.setCallbackData(startOfCallbackCommand + dateFormatter.format(dateNow.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))));
        rows.add(List.of(saturdayButton));

        InlineKeyboardButton sundayButton = new InlineKeyboardButton();
        sundayButton.setText("${command.remind.onsunday}");
        sundayButton.setCallbackData(startOfCallbackCommand + dateFormatter.format(dateNow.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))));
        rows.add(List.of(sundayButton));

        return rows;
    }

    private List<List<InlineKeyboardButton>> prepareButtonRowsWithTimeSettings(Reminder reminder) {
        Long reminderId = reminder.getId();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String startOfCallbackCommand = CALLBACK_SET_REMINDER + reminderId +
                    SET_DATE + dateFormatter.format(reminder.getDate()) + SET_TIME;

        if (reminder.getTime() != null) {
            InlineKeyboardButton storedTimeButton = new InlineKeyboardButton();
            String formattedTime = timeShortFormatter.format(reminder.getTime());
            storedTimeButton.setText("${command.remind.leave} " + formattedTime);
            storedTimeButton.setCallbackData(startOfCallbackCommand + formattedTime);
            rows.add(List.of(storedTimeButton));
        }

        InlineKeyboardButton morningButton = new InlineKeyboardButton();
        String morningTime = "07:00";
        morningButton.setText("${command.remind.morning} " + morningTime);
        morningButton.setCallbackData(startOfCallbackCommand + morningTime);
        rows.add(List.of(morningButton));

        InlineKeyboardButton lunchButton = new InlineKeyboardButton();
        String lunchTime = "13:00";
        lunchButton.setText("${command.remind.lunch} " + lunchTime);
        lunchButton.setCallbackData(startOfCallbackCommand + lunchTime);
        rows.add(List.of(lunchButton));

        InlineKeyboardButton dinnerButton = new InlineKeyboardButton();
        String dinnerTime = "18:00";
        dinnerButton.setText("${command.remind.dinner} " + dinnerTime);
        dinnerButton.setCallbackData(startOfCallbackCommand + dinnerTime);
        rows.add(List.of(dinnerButton));

        InlineKeyboardButton eveningButton = new InlineKeyboardButton();
        String eveningTime = "20:00";
        eveningButton.setText("${command.remind.evening} " + eveningTime);
        eveningButton.setCallbackData(startOfCallbackCommand + eveningTime);
        rows.add(List.of(eveningButton));

        InlineKeyboardButton nightButton = new InlineKeyboardButton();
        String nightTime = "03:00";
        nightButton.setText("${command.remind.night} " + nightTime);
        nightButton.setCallbackData(startOfCallbackCommand + nightTime);
        rows.add(List.of(nightButton));

        return rows;
    }

    private static List<List<InlineKeyboardButton>> prepareRepeatKeyboard(Reminder reminder) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(
                Stream.of(
                        ReminderRepeatability.MINUTES1,
                        ReminderRepeatability.MINUTES5,
                        ReminderRepeatability.MINUTES10,
                        ReminderRepeatability.MINUTES15,
                        ReminderRepeatability.MINUTES30)
                    .map(reminderRepeatability -> generateRepeatButton(reminderRepeatability, reminder))
                    .collect(Collectors.toList()));

        rows.add(
                Stream.of(
                        ReminderRepeatability.HOURS1,
                        ReminderRepeatability.HOURS2,
                        ReminderRepeatability.HOURS3,
                        ReminderRepeatability.HOURS6,
                        ReminderRepeatability.HOURS12)
                .map(reminderRepeatability -> generateRepeatButton(reminderRepeatability, reminder))
                .collect(Collectors.toList()));

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
                .collect(Collectors.toList()));

        rows.add(
                Stream.of(
                        ReminderRepeatability.DAY,
                        ReminderRepeatability.WEEK,
                        ReminderRepeatability.MONTH,
                        ReminderRepeatability.YEAR)
                .map(reminderRepeatability -> generateRepeatButton(reminderRepeatability, reminder))
                .collect(Collectors.toList()));

        return rows;
    }

    private static InlineKeyboardButton generateRepeatButton(ReminderRepeatability reminderRepeatability, Reminder reminder) {
        String callbackData;
        String currentRepeatability = reminder.getRepeatability();
        String ordinal = String.valueOf(reminderRepeatability.ordinal());
        if (StringUtils.isEmpty(currentRepeatability)) {
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

        InlineKeyboardButton setRepeatButton = new InlineKeyboardButton();
        setRepeatButton.setText(reminderRepeatability.getCaption());
        setRepeatButton.setCallbackData(callbackData);

        return setRepeatButton;
    }

    private InlineKeyboardMarkup prepareKeyboardWithReminderInfo(Reminder reminder) {
        return prepareKeyboardWithReminderInfo(new ArrayList<>(), reminder);
    }

    private InlineKeyboardMarkup prepareKeyboardWithReminderInfo(List<List<InlineKeyboardButton>> rows, Reminder reminder) {
        addMainRows(rows, reminder, false);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private PartialBotApiMethod<?> addReminderByCallback(Message message, Chat chat, User user) {
        log.debug("Request to add reminder after callback");
        commandWaitingService.add(chat, user, Remind.class, CALLBACK_COMMAND);

        final String addingHelpText = "\n${command.remind.commandwaitingstart}";

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.disableWebPagePreview();
        editMessageText.setText(addingHelpText);

        return editMessageText;
    }

    private PartialBotApiMethod<?> deleteReminderByCallback(Message message, Chat chat, User user, String command) {
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

        Reminder reminder = reminderService.get(chat, user, reminderId);
        if (reminder == null) {
            return generateDeleteMessage(chat, message);
        }

        try {
            reminderService.remove(reminder);
        } catch (Exception ignored) {
            // failed to delete (mb not owner)
        }

        if (deleteReminderMessage) {
            return generateDeleteMessage(chat, message);
        }

        return getReminderListWithKeyboard(message, chat, user, FIRST_PAGE, false);
    }

    private PartialBotApiMethod<?> getReminderListWithKeyboard(Message message, Chat chat, User user, int page, boolean newMessage) {
        log.debug("Request to list all reminders for chat {} and user {}, page: {}", chat.getChatId(), user.getUserId(), page);
        Page<Reminder> reminderList = reminderService.getByChatAndUser(chat, user, page);

        String languageCode = languageResolver.getChatLanguageCode(message, user);
        String caption = TextUtils.getLinkToUser(user, true) + "<b> ${command.remind.yourreminders}:</b>\n" + buildTextReminderList(reminderList.toList(), languageCode);

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.enableHtml(true);
            sendMessage.disableWebPagePreview();
            sendMessage.setText(caption);
            sendMessage.setReplyMarkup(prepareKeyboardWithRemindersForSetting(reminderList, page, languageCode));

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.disableWebPagePreview();
        editMessageText.setText(caption);
        editMessageText.setReplyMarkup(prepareKeyboardWithRemindersForSetting(reminderList, page, languageCode));

        return editMessageText;
    }

    private String buildTextReminderList(List<Reminder> reminderList, String lang) {
        return reminderList
                .stream()
                .map(reminder -> {
                    LocalDateTime reminderDateTime = reminder.getDate().atTime(reminder.getTime());
                    String additionally = StringUtils.isEmpty(reminder.getRepeatability()) ? "" : Emoji.CALENDAR.getSymbol();
                    additionally = " " + getConditionalEmoji(reminder) + additionally;

                    return DateUtils.formatShortDateTime(reminderDateTime) + " (" + getDayOfWeek(reminderDateTime, lang) + " )" +
                            " — " + reminder.getText() + additionally;
                })
                .collect(Collectors.joining("\n"));
    }

    private DeleteMessage closeReminderMenu(Message message, Chat chat, User user, String command) {
        log.debug("Request to close reminder menu by messageId {}", message.getMessageId());

        String userId = command.substring(CLOSE_REMINDER_MENU.length());
        if (user.getUserId().toString().equals(userId)) {
            return generateDeleteMessage(chat, message);
        }

        return null;
    }

    private DeleteMessage generateDeleteMessage(Chat chat, Message message) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chat.getChatId());
        deleteMessage.setMessageId(message.getMessageId());

        return deleteMessage;
    }

    private InlineKeyboardMarkup prepareKeyboardWithRemindersForSetting(Page<Reminder> reminderList, int page, String lang) {
        final int maxButtonTextLength = 14;

        List<List<InlineKeyboardButton>> rows = reminderList.stream().map(reminder -> {
            List<InlineKeyboardButton> remindersRow = new ArrayList<>();

            String reminderText = internationalizationService.internationalize(reminder.getText(), lang);
            if (reminderText.length() > 14) {
                reminderText = reminderText.substring(0, maxButtonTextLength - 3) + "...";
            }

            reminderText = getConditionalEmoji(reminder) + reminderText;

            InlineKeyboardButton reminderButton = new InlineKeyboardButton();
            reminderButton.setText(reminderText);
            reminderButton.setCallbackData(CALLBACK_INFO_REMINDER + reminder.getId());

            remindersRow.add(reminderButton);

            return remindersRow;
        }).collect(Collectors.toList());

        addingMainRows(rows, page, reminderList.getTotalPages());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private String getConditionalEmoji(Reminder reminder) {
        if (Boolean.TRUE.equals(reminder.getNotified())) {
            return Emoji.NO_BELL.getSymbol();
        } else {
            return Emoji.BELL.getSymbol();
        }
    }

    private void addingMainRows(List<List<InlineKeyboardButton>> rows, int page, int totalPages) {
        List<InlineKeyboardButton> pagesRow = new ArrayList<>();

        if (page > 0) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText(Emoji.LEFT_ARROW.getSymbol() + "${command.remind.button.back}");
            backButton.setCallbackData(CALLBACK_COMMAND + SELECT_PAGE + (page - 1));

            pagesRow.add(backButton);
        }

        if (page + 1 < totalPages) {
            InlineKeyboardButton forwardButton = new InlineKeyboardButton();
            forwardButton.setText("${command.remind.button.forward}" + Emoji.RIGHT_ARROW.getSymbol());
            forwardButton.setCallbackData(CALLBACK_COMMAND + SELECT_PAGE + (page + 1));

            pagesRow.add(forwardButton);
        }

        rows.add(pagesRow);

        List<InlineKeyboardButton> addButtonRow = new ArrayList<>();
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText(Emoji.NEW.getSymbol() + "${command.remind.button.add}");
        addButton.setCallbackData(CALLBACK_ADD_COMMAND);
        addButtonRow.add(addButton);

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getSymbol() + "${command.remind.button.reload}");
        updateButton.setCallbackData(CALLBACK_UPDATE_COMMAND);
        updateButtonRow.add(updateButton);

        rows.add(addButtonRow);
        rows.add(updateButtonRow);
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

    public static InlineKeyboardMarkup preparePostponeKeyboard(Reminder reminder, Locale locale) {
        Long reminderId = reminder.getId();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(Stream.of(1, 5, 10, 15, 30)
                .map(minutes -> generatePostponeButton(reminderId, minutes + " ${command.remind.m}.", Duration.of(minutes, MINUTES)))
                .collect(Collectors.toList()));

        rows.add(Stream.of(1, 2, 3, 6, 12)
                .map(hours -> generatePostponeButton(reminderId, hours + " ${command.remind.h}.", Duration.of(hours, HOURS)))
                .collect(Collectors.toList()));

        LocalDate dateNow = LocalDate.now();
        rows.add(Arrays.stream(DayOfWeek.values())
                .map(dayOfWeek -> generatePostponeButton(
                        reminderId,
                        dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                        Period.between(dateNow, dateNow.with(TemporalAdjusters.next(dayOfWeek)))))
                .collect(Collectors.toList()));

        rows.add(
                List.of(
                        generatePostponeButton(reminderId, "${command.remind.day}", Period.ofDays(1)),
                        generatePostponeButton(reminderId, "${command.remind.week}", Period.ofWeeks(1)),
                        generatePostponeButton(reminderId, "${command.remind.month}", Period.ofMonths(1)),
                        generatePostponeButton(reminderId, "${command.remind.year}", Period.ofYears(1))));

        addMainRows(rows, reminder, true);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private static InlineKeyboardButton generatePostponeButton(Long reminderId, String text, TemporalAmount amount) {
        String startOfCallbackCommand = CALLBACK_SET_REMINDER + reminderId;

        InlineKeyboardButton postponeButton = new InlineKeyboardButton();
        postponeButton.setText(text);
        postponeButton.setCallbackData(startOfCallbackCommand + amount);

        return postponeButton;
    }

    private String cutKeyWordInText(String text, Set<String> keywords) {
        String foundKeyword = keywords
                .stream()
                .filter(text::contains)
                .findFirst()
                .orElseThrow(() -> new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)));

        return text.replaceFirst(foundKeyword, "");
    }

    private static void addMainRows(List<List<InlineKeyboardButton>> rows, Reminder reminder, boolean fromPostponeMenu) {
        Long reminderId = reminder.getId();

        InlineKeyboardButton setReminderButton = new InlineKeyboardButton();
        setReminderButton.setText(Emoji.SETTINGS.getSymbol());
        setReminderButton.setCallbackData(CALLBACK_SET_REMINDER + reminderId);

        String deleteCommand = CALLBACK_DELETE_COMMAND + reminderId;
        if (fromPostponeMenu) {
            deleteCommand = deleteCommand + CLOSE_REMINDER_MENU;
        }
        InlineKeyboardButton deleteReminderButton = new InlineKeyboardButton();
        deleteReminderButton.setText(Emoji.DELETE.getSymbol());
        deleteReminderButton.setCallbackData(deleteCommand);

        String notifiedCaption;
        if (Boolean.TRUE.equals(reminder.getNotified())) {
            notifiedCaption = Emoji.PLAY_BUTTON.getSymbol();
        } else {
            notifiedCaption = Emoji.STOP_BUTTON.getSymbol();
        }
        InlineKeyboardButton disableReminderButton = new InlineKeyboardButton();
        disableReminderButton.setText(notifiedCaption);
        disableReminderButton.setCallbackData(CALLBACK_SET_REMINDER + reminderId + SET_NOTIFIED);

        String repeatability = reminder.getRepeatability() == null ? "" : reminder.getRepeatability();
        InlineKeyboardButton repeatReminderButton = new InlineKeyboardButton();
        repeatReminderButton.setText(Emoji.CALENDAR.getSymbol());
        repeatReminderButton.setCallbackData(CALLBACK_SET_REMINDER + reminderId + SET_REPEATABLE + repeatability);

        InlineKeyboardButton updateReminderButton = new InlineKeyboardButton();
        updateReminderButton.setText(Emoji.UPDATE.getSymbol());
        updateReminderButton.setCallbackData(CALLBACK_INFO_REMINDER + reminderId);

        InlineKeyboardButton okButton = new InlineKeyboardButton();
        okButton.setText(Emoji.CHECK_MARK_BUTTON.getSymbol());
        okButton.setCallbackData(CALLBACK_CLOSE_REMINDER_MENU + reminder.getUser().getUserId());

        rows.add(
                List.of(
                        setReminderButton,
                        deleteReminderButton,
                        disableReminderButton,
                        repeatReminderButton,
                        updateReminderButton,
                        okButton));

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.LEFT_ARROW.getSymbol() + "${command.remind.button.back}");
        backButton.setCallbackData(CALLBACK_COMMAND);
        rows.add(List.of(backButton));
    }

}
