package org.telegram.bot.domain.commands;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.domain.enums.ReminderRepeatability;
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

import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.Set;
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
public class Remind implements CommandParent<PartialBotApiMethod<?>> {

    private final ReminderService reminderService;
    private final CommandWaitingService commandWaitingService;
    private final UserService userService;
    private final UserCityService userCityService;
    private final SpeechService speechService;
    private final BotStats botStats;

    private static final String CALLBACK_COMMAND = "напомнить ";
    private static final String DELETE_COMMAND = "удалить";
    private static final String CALLBACK_DELETE_COMMAND = CALLBACK_COMMAND + DELETE_COMMAND;
    private static final String ADD_COMMAND = "добавить";
    private static final String CALLBACK_ADD_COMMAND = CALLBACK_COMMAND + ADD_COMMAND;
    private static final String UPDATE_COMMAND = "обновить";
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
    private static final Pattern AFTER_MINUTES_PATTERN = Pattern.compile("через\\s+(\\d+)\\s+((\\bминуту\\b)|(\\bминуты\\b)|(\\bминут\\b))");
    private static final Pattern AFTER_HOURS_PATTERN = Pattern.compile("через\\s+(\\d+)\\s+((\\bчас\\b)|(\\bчаса\\b)|(\\bчасов\\b))");
    private static final Pattern AFTER_DAYS_PATTERN = Pattern.compile("через\\s+(\\d+)\\s+((\\bдень\\b)|(\\bдня\\b)|(\\bдней\\b))");
    private static final Pattern ID_PATTERN = Pattern.compile("s\\d+");
    private static final Pattern SET_ID_PATTERN = Pattern.compile(SET_REMINDER + "\\d+");
    private static final Pattern SET_NOTIFIED_PATTERN = Pattern.compile(SET_REMINDER + "\\d+" + SET_NOTIFIED);
    private static final Pattern SET_REPEATABLE_PATTERN = Pattern.compile(SET_REMINDER + "\\d+" + SET_REPEATABLE);
    private static final Pattern POSTPONE_PATTERN = Pattern.compile("P(?!$)(\\d+Y)?(\\d+M)?(\\d+W)?(\\d+D)?(T(?=\\d)(\\d+H)?(\\d+M)?(\\d+S)?)?");

    @Override
    public PartialBotApiMethod<?> parse(Update update) {
        Message message = getMessageFromUpdate(update);
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

    private EditMessageText getReminderInfo(Message message, Chat chat, User user, String command) {
        long reminderId;
        try {
            reminderId = Long.parseLong(command.substring(INFO_REMINDER.length()));
        } catch (NumberFormatException e) {
            botStats.incrementErrors();
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        Reminder reminder = reminderService.get(chat, user, reminderId);
        if (reminder == null) {
            return null;
        }

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(message.getChatId().toString());
        editMessage.setMessageId(message.getMessageId());
        editMessage.enableHtml(true);
        editMessage.setText(prepareReminderInfoText(reminder));
        editMessage.setReplyMarkup(prepareKeyboardWithReminderInfo(reminder));

        return editMessage;
    }

    private PartialBotApiMethod<?> addReminder(Message message, Chat chat, User user, String command) {
        log.debug("Request to add new reminder: {}", command);

        String reminderText;
        LocalDate reminderDate = null;
        LocalTime reminderTime = null;

        Pattern datePattern = FULL_DATE_PATTERN;
        Matcher matcher = datePattern.matcher(command);
        if (matcher.find()) {
            reminderDate = getDateFromText(command.substring(matcher.start(), matcher.end()));
        } else {
            datePattern = SHORT_DATE_PATTERN;
            matcher = datePattern.matcher(command);
            if (matcher.find()) {
                try {
                    reminderDate = getDateFromText(command.substring(matcher.start(), matcher.end()), LocalDate.now().getYear());
                } catch (Exception ignored) {
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
                TimeKeyword timeKeyword = TimeKeyword.getKeyWordFromText(command);
                if (timeKeyword != null) {
                    reminderTime = timeKeyword.getDateSupplier().get();
                    command = cutKeyWordInText(command, timeKeyword.getKeywords());
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
                    reminderDate = LocalDate.now().plusDays(days);
                    command = command.replaceFirst(afterDaysPhrase, "").trim();
                }
            }

            if (reminderDate == null) {
                DateKeyword dateKeyword = DateKeyword.getKeyWordFromText(command);
                if (dateKeyword != null) {
                    reminderDate = dateKeyword.dateSupplier.get();
                    command = command.replaceFirst(dateKeyword.getKeyword(), "").trim();
                }
            }

            if (reminderDate == null) {
                LocalDate dateNow = LocalDate.now();
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
        sendMessage.setText(prepareReminderInfoText(reminder));
        sendMessage.setReplyMarkup(prepareKeyboardWithReminderInfo(reminder));

        return sendMessage;
    }

    private String getAfterMinutesPhrase(String text) {
        if (text == null) {
            return null;
        }

        Matcher matcher = AFTER_MINUTES_PATTERN.matcher(text);

        if (matcher.find()) {
            return text.substring(matcher.start(), matcher.end());
        }

        return null;
    }

    private String getAfterHoursPhrase(String text) {
        if (text == null) {
            return null;
        }

        Matcher matcher = AFTER_HOURS_PATTERN.matcher(text);

        if (matcher.find()) {
            return text.substring(matcher.start(), matcher.end());
        }

        return null;
    }

    private String getAfterDaysPhrase(String text) {
        if (text == null) {
            return null;
        }

        Matcher matcher = AFTER_DAYS_PATTERN.matcher(text);

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
            }

            if (reminder != null) {
                matcher = SET_FULL_DATE_PATTERN.matcher(command);
                if (matcher.find()) {
                    reminder.setDate(getDateFromText(command.substring(matcher.start() + 1, matcher.end())));
                } else {
                    matcher = SET_SHORT_DATE_PATTERN.matcher(command);
                    if (matcher.find()) {
                        reminder.setDate(getDateFromText(command.substring(matcher.start() + 1, matcher.end()), LocalDate.now().getYear()));
                    } else {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                }

                if (command.indexOf(SET_TIME) < 1) {
                    commandWaitingService.add(chat, user, Remind.class, CALLBACK_COMMAND + SET_REMINDER + reminder.getId() +
                            SET_DATE + dateFormatter.format(reminder.getDate()) + SET_TIME);
                    keyboard = prepareKeyboardWithReminderInfo(prepareButtonRowsWithTimeSettings(reminder), reminder);
                    caption = "Выберите или введите вручную ЧЧ:ММ\n";

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
        sendMessage.setText(prepareReminderInfoText(reminder) + "<b>" + caption + "</b>");
        sendMessage.setReplyMarkup(keyboard);

        return sendMessage;
    }

    private EditMessageText setReminderByCallback(Message message, Chat chat, User user, String command) {
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
                return null;
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
            if (reminder.getNotified()) {
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
            caption = "Повторять каждые: ";
            rowsWithButtons = prepareRepeatKeyboard(reminder);
        } else if (setFullDateMatcher.find()) {
            reminder.setDate(getDateFromText(command.substring(setFullDateMatcher.start() + 1, setFullDateMatcher.end())));
            if (setTimeMatcher.find()) {
                reminder.setTime(getTimeFromText(command.substring(setTimeMatcher.start() + 1, setTimeMatcher.end())))
                        .setNotified(false);
            } else {
                caption = "Выберите или введите вручную ЧЧ:ММ\n";
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
                            .setText("(копия) " + reminder.getText())
                            .setNotified(false);
                }
            } else {
                caption = "Выберите или введите вручную ДД.ММ или ДД.ММ.ГГГГ\n";
                rowsWithButtons = prepareButtonRowsWithDateSetting(reminder);
                commandWaitingService.add(chat, user, Remind.class, CALLBACK_COMMAND + SET_REMINDER + reminder.getId() +
                        SET_DATE);
            }
        }

        reminderService.save(reminder);

        InlineKeyboardMarkup keyboard = prepareKeyboardWithReminderInfo(rowsWithButtons, reminder);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(prepareReminderInfoText(reminder) + "<b>" + caption + "</b>");
        editMessageText.setReplyMarkup(keyboard);

        return editMessageText;
    }

    private LocalDate getDateFromText(String text, int year) {
        return getDateFromText(text + "." + year);
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

    private String prepareReminderInfoText(Reminder reminder) {
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
            repeatability = "без повтора";
        } else {
            repeatability = getRepeatabilityInfo(reminderRepeatability);
        }

        String deltaDates = deltaDatesToString(reminderDateTime, dateTimeNow);
        String leftToRun;
        if (dateTimeNow.isAfter(reminderDateTime) && reminder.getNotified()) {
            leftToRun = "Сработало: <b>" + deltaDates + "</b>назад\n";
        } else if (dateTimeNow.isAfter(reminderDateTime) && !reminder.getNotified()) {
            leftToRun = "До срабатывания: <b> ща всё будет </b>\n";
        } else if (dateTimeNow.isBefore(reminderDateTime) && reminder.getNotified()) {
            leftToRun = "Отключено\n";
        } else {
            leftToRun = "До срабатывания: <b>" + deltaDates + "</b>\n";
        }

        return "<b>Напоминание</b>\n" +
                reminder.getText() + "\n<i>" +
                formatDate(reminderDate) + " " + formatTime(reminderTime) + " (" + getDayOfWeek(reminderDate) + " )</i>\n" +
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
            storedDateButton.setText("Оставить " + formattedDate);
            storedDateButton.setCallbackData(startOfCallbackCommand + formattedDate);
            rows.add(List.of(storedDateButton));
        }

        InlineKeyboardButton todayButton = new InlineKeyboardButton();
        todayButton.setText("Сегодня");
        todayButton.setCallbackData(startOfCallbackCommand + dateFormatter.format(dateNow));
        rows.add(List.of(todayButton));

        InlineKeyboardButton tomorrowButton = new InlineKeyboardButton();
        tomorrowButton.setText("Завтра");
        tomorrowButton.setCallbackData(startOfCallbackCommand + dateFormatter.format(dateNow.plusDays(1)));
        rows.add(List.of(tomorrowButton));

        InlineKeyboardButton afterTomorrowButton = new InlineKeyboardButton();
        afterTomorrowButton.setText("Послезавтра");
        afterTomorrowButton.setCallbackData(startOfCallbackCommand + dateFormatter.format(dateNow.plusDays(2)));
        rows.add(List.of(afterTomorrowButton));

        InlineKeyboardButton saturdayButton = new InlineKeyboardButton();
        saturdayButton.setText("В субботу");
        saturdayButton.setCallbackData(startOfCallbackCommand + dateFormatter.format(dateNow.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))));
        rows.add(List.of(saturdayButton));

        InlineKeyboardButton sundayButton = new InlineKeyboardButton();
        sundayButton.setText("В воскресенье");
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
            storedTimeButton.setText("Оставить " + formattedTime);
            storedTimeButton.setCallbackData(startOfCallbackCommand + formattedTime);
            rows.add(List.of(storedTimeButton));
        }

        InlineKeyboardButton morningButton = new InlineKeyboardButton();
        String morningTime = "07:00";
        morningButton.setText("Утром " + morningTime);
        morningButton.setCallbackData(startOfCallbackCommand + morningTime);
        rows.add(List.of(morningButton));

        InlineKeyboardButton lunchButton = new InlineKeyboardButton();
        String lunchTime = "13:00";
        lunchButton.setText("В обед " + lunchTime);
        lunchButton.setCallbackData(startOfCallbackCommand + lunchTime);
        rows.add(List.of(lunchButton));

        InlineKeyboardButton dinnerButton = new InlineKeyboardButton();
        String dinnerTime = "18:00";
        dinnerButton.setText("Вечером " + dinnerTime);
        dinnerButton.setCallbackData(startOfCallbackCommand + dinnerTime);
        rows.add(List.of(dinnerButton));

        InlineKeyboardButton eveningButton = new InlineKeyboardButton();
        String eveningTime = "20:00";
        eveningButton.setText("Поздно вечером " + eveningTime);
        eveningButton.setCallbackData(startOfCallbackCommand + eveningTime);
        rows.add(List.of(eveningButton));

        InlineKeyboardButton nightButton = new InlineKeyboardButton();
        String nightTime = "03:00";
        nightButton.setText("Ночью " + nightTime);
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
        addMainRows(rows, reminder);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private PartialBotApiMethod<?> addReminderByCallback(Message message, Chat chat, User user) {
        log.debug("Request to add reminder after callback");
        commandWaitingService.add(chat, user, Remind.class, CALLBACK_COMMAND);

        final String addingHelpText = "\nНапиши мне текст напоминания";

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(addingHelpText);

        return editMessageText;
    }

    private PartialBotApiMethod<?> deleteReminderByCallback(Message message, Chat chat, User user, String command) {
        log.debug("Request to delete reminder");

        if (command.equals(DELETE_COMMAND)) {
            return getReminderListWithKeyboard(message, chat, user, FIRST_PAGE, false);
        }

        long reminderId;
        try {
            reminderId = Long.parseLong(command.substring(DELETE_COMMAND.length()));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        Reminder reminder = reminderService.get(chat, user, reminderId);
        if (reminder == null) {
            return null;
        }

        try {
            reminderService.remove(reminder);
        } catch (Exception ignored) {}

        return getReminderListWithKeyboard(message, chat, user, FIRST_PAGE, false);
    }

    private PartialBotApiMethod<?> getReminderListWithKeyboard(Message message, Chat chat, User user, int page, boolean newMessage) {
        log.debug("Request to list all reminders for chat {} and user {}, page: {}", chat.getChatId(), user.getUserId(), page);
        Page<Reminder> reminderList = reminderService.getByChatAndUser(chat, user, page);

        String caption = TextUtils.getLinkToUser(user, true) + "<b> твои напоминания:</b>\n" + buildTextReminderList(reminderList.toList());

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.enableHtml(true);
            sendMessage.setText(caption);
            sendMessage.setReplyMarkup(prepareKeyboardWithRemindersForSetting(reminderList, page));

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(caption);
        editMessageText.setReplyMarkup(prepareKeyboardWithRemindersForSetting(reminderList, page));

        return editMessageText;
    }

    private String buildTextReminderList(List<Reminder> reminderList) {
        return reminderList
                .stream()
                .map(reminder -> {
                    LocalDateTime reminderDateTime = reminder.getDate().atTime(reminder.getTime());
                    String additionally = StringUtils.isEmpty(reminder.getRepeatability()) ? "" : Emoji.CALENDAR.getEmoji();
                    additionally = " " + getConditionalEmoji(reminder) + additionally;

                    return DateUtils.formatShortDateTime(reminderDateTime) + " (" + getDayOfWeek(reminderDateTime) + " )" +
                            " — " + reminder.getText() + additionally;
                })
                .collect(Collectors.joining("\n"));
    }

    private DeleteMessage closeReminderMenu(Message message, Chat chat, User user, String command) {
        Integer messageId = message.getMessageId();
        log.debug("Request to close reminder menu by messageId {}", messageId);

        String userId = command.substring(CLOSE_REMINDER_MENU.length());
        if (user.getUserId().toString().equals(userId)) {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(chat.getChatId());
            deleteMessage.setMessageId(messageId);

            return deleteMessage;
        }

        return null;
    }

    private InlineKeyboardMarkup prepareKeyboardWithRemindersForSetting(Page<Reminder> reminderList, int page) {
        final int maxButtonTextLength = 14;

        List<List<InlineKeyboardButton>> rows = reminderList.stream().map(reminder -> {
            List<InlineKeyboardButton> remindersRow = new ArrayList<>();

            String reminderText = reminder.getText();
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
        if (reminder.getNotified()) {
            return Emoji.NO_BELL.getEmoji();
        } else {
            return Emoji.BELL.getEmoji();
        }
    }

    private void addingMainRows(List<List<InlineKeyboardButton>> rows, int page, int totalPages) {
        List<InlineKeyboardButton> pagesRow = new ArrayList<>();

        if (page > 0) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText(Emoji.LEFT_ARROW.getEmoji() + "Назад");
            backButton.setCallbackData(CALLBACK_COMMAND + SELECT_PAGE + (page - 1));

            pagesRow.add(backButton);
        }

        if (page + 1 < totalPages) {
            InlineKeyboardButton forwardButton = new InlineKeyboardButton();
            forwardButton.setText("Вперёд" + Emoji.RIGHT_ARROW.getEmoji());
            forwardButton.setCallbackData(CALLBACK_COMMAND + SELECT_PAGE + (page + 1));

            pagesRow.add(forwardButton);
        }

        rows.add(pagesRow);

        List<InlineKeyboardButton> addButtonRow = new ArrayList<>();
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText(Emoji.NEW.getEmoji() + "Добавить");
        addButton.setCallbackData(CALLBACK_ADD_COMMAND);
        addButtonRow.add(addButton);

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getEmoji() + "Обновить");
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
        return "<b>Напоминание</b>\n" +
                reminder.getText() + "\n<i>" +
                formatDate(reminder.getDate()) + " " + formatTime(reminder.getTime()) + "</i>\n" +
                "Отложить на:\n";
    }

    public static InlineKeyboardMarkup preparePostponeKeyboard(Reminder reminder) {
        Long reminderId = reminder.getId();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(Stream.of(1, 5, 10, 15, 30)
                .map(minutes -> generatePostponeButton(reminderId, minutes + " м.", Duration.of(minutes, MINUTES)))
                .collect(Collectors.toList()));

        rows.add(Stream.of(1, 2, 3, 6, 12)
                .map(hours -> generatePostponeButton(reminderId, hours + " ч.", Duration.of(hours, HOURS)))
                .collect(Collectors.toList()));

        Locale ruLocale = new Locale("ru");
        LocalDate dateNow = LocalDate.now();
        rows.add(Arrays.stream(DayOfWeek.values())
                .map(dayOfWeek -> generatePostponeButton(
                        reminderId,
                        dayOfWeek.getDisplayName(TextStyle.SHORT, ruLocale),
                        Period.between(dateNow, dateNow.with(TemporalAdjusters.next(dayOfWeek)))))
                .collect(Collectors.toList()));

        rows.add(
                List.of(
                        generatePostponeButton(reminderId, "День", Period.ofDays(1)),
                        generatePostponeButton(reminderId, "Неделю", Period.ofWeeks(1)),
                        generatePostponeButton(reminderId, "Месяц", Period.ofMonths(1)),
                        generatePostponeButton(reminderId, "Год", Period.ofYears(1))));

        addMainRows(rows, reminder);

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

    private static void addMainRows(List<List<InlineKeyboardButton>> rows, Reminder reminder) {
        Long reminderId = reminder.getId();

        InlineKeyboardButton setReminderButton = new InlineKeyboardButton();
        setReminderButton.setText(Emoji.SETTINGS.getEmoji());
//        setReminderButton.setText(Emoji.SETTINGS.getEmoji() + "Дата/время");
        setReminderButton.setCallbackData(CALLBACK_SET_REMINDER + reminderId);
//        rows.add(List.of(setReminderButton));

        InlineKeyboardButton deleteReminderButton = new InlineKeyboardButton();
//        deleteReminderButton.setText(Emoji.DELETE.getEmoji() + "Удалить");
        deleteReminderButton.setText(Emoji.DELETE.getEmoji());
        deleteReminderButton.setCallbackData(CALLBACK_DELETE_COMMAND + reminderId);
//        rows.add(List.of(deleteReminderButton));

        String notifiedCaption;
        if (reminder.getNotified()) {
//            notifiedCaption = Emoji.PLAY_BUTTON.getEmoji() + "Включить";
            notifiedCaption = Emoji.PLAY_BUTTON.getEmoji();
        } else {
//            notifiedCaption = Emoji.STOP_BUTTON.getEmoji() + "Отключить";
            notifiedCaption = Emoji.STOP_BUTTON.getEmoji();
        }
        InlineKeyboardButton disableReminderButton = new InlineKeyboardButton();
        disableReminderButton.setText(notifiedCaption);
        disableReminderButton.setCallbackData(CALLBACK_SET_REMINDER + reminderId + SET_NOTIFIED);
//        rows.add(List.of(disableReminderButton));

        String repeatability = reminder.getRepeatability() == null ? "" : reminder.getRepeatability();
        InlineKeyboardButton repeatReminderButton = new InlineKeyboardButton();
        repeatReminderButton.setText(Emoji.CALENDAR.getEmoji());
//        repeatReminderButton.setText(Emoji.CALENDAR.getEmoji() + "Повторять");
        repeatReminderButton.setCallbackData(CALLBACK_SET_REMINDER + reminderId + SET_REPEATABLE + repeatability);
//        rows.add(List.of(repeatReminderButton));

        InlineKeyboardButton updateReminderButton = new InlineKeyboardButton();
//        updateReminderButton.setText(Emoji.UPDATE.getEmoji() + "Обновить");
        updateReminderButton.setText(Emoji.UPDATE.getEmoji());
        updateReminderButton.setCallbackData(CALLBACK_INFO_REMINDER + reminderId);
//        rows.add(List.of(updateReminderButton));

        InlineKeyboardButton okButton = new InlineKeyboardButton();
        okButton.setText(Emoji.CHECK_MARK_BUTTON.getEmoji());
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
        backButton.setText(Emoji.LEFT_ARROW.getEmoji() + "Назад");
        backButton.setCallbackData(CALLBACK_COMMAND);
        rows.add(List.of(backButton));
    }

    @RequiredArgsConstructor
    @Getter
    private enum DateKeyword {
        TODAY("сегодня", LocalDate::now),
        TOMORROW("завтра", () -> LocalDate.now().plusDays(1)),
        AFTER_TOMORROW("послезавтра", () -> LocalDate.now().plusDays(2)),
        MONDAY("в понедельник", () -> LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))),
        TUESDAY("во вторник", () -> LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY))),
        WEDNESDAY("в среду", () -> LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))),
        THURSDAY("в четверг", () -> LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.THURSDAY))),
        FRIDAY("в пятницу", () -> LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.FRIDAY))),
        SATURDAY("в субботу", () -> LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY))),
        SUNDAY("в воскресенье", () -> LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY))),
        ;

        private final String keyword;
        private final Supplier<LocalDate> dateSupplier;

        public static DateKeyword getKeyWordFromText(String text) {
            return Arrays.stream(DateKeyword.values())
                    .filter(dateKeyword -> text.contains(dateKeyword.getKeyword()))
                    .findFirst()
                    .orElse(null);
        }
    }

    @RequiredArgsConstructor
    @Getter
    private enum TimeKeyword {
        MORNING(Set.of("утром"), () -> LocalTime.of(7, 0)),
        LUNCH(Set.of("в обед", "к обеду"), () -> LocalTime.of(12, 0)),
        AFTERNOON(Set.of("днём", "днем"), () -> LocalTime.of(15, 0)),
        EVENING(Set.of("поздним вечером", "поздно вечером"), () -> LocalTime.of(22, 0)),
        DINNER(Set.of("вечером"), () -> LocalTime.of(19, 0)),
        NIGHT(Set.of("ночью"), () -> LocalTime.of(3, 0)),
        ;

        private final java.util.Set<String> keywords;
        private final Supplier<LocalTime> dateSupplier;

        public static TimeKeyword getKeyWordFromText(String text) {
            return Arrays.stream(TimeKeyword.values())
                    .filter(timeKeyword -> timeKeyword.getKeywords().stream().anyMatch(text::contains))
                    .findFirst()
                    .orElse(null);
        }
    }
}
