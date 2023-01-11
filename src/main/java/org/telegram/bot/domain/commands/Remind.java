package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class Remind implements CommandParent<PartialBotApiMethod<?>> {

    private final ReminderService reminderService;
    private final CommandWaitingService commandWaitingService;
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
    private static final String SET_DATE = "d";
    private static final String SET_TIME = "t";
    private static final String SET_POSTPONE = "p";
    private static final int FIRST_PAGE = 0;

    private static final Pattern fullDatePattern = Pattern.compile(SET_DATE + "(\\d{2})\\.(\\d{2})\\.(\\d{4})");
    private static final Pattern shortDatePattern = Pattern.compile(SET_DATE + "(\\d{2})\\.(\\d{2})");
    private static final Pattern timePattern = Pattern.compile(SET_TIME + "(\\d{2}):(\\d{2})");

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

        if (callback) {
            User user = new User().setUserId(update.getCallbackQuery().getFrom().getId());

            if (StringUtils.isEmpty(textMessage)) {
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
            }
        }

        User user = new User().setUserId(message.getFrom().getId());
        if (textMessage == null || textMessage.equals(emptyCommand)) {
            return getReminderListWithKeyboard(message, chat, user, FIRST_PAGE, true);
        } else if (textMessage.startsWith(SET_REMINDER) && commandWaiting != null) {
            return manualReminderEdit(message, chat, user, commandWaiting, textMessage);
        } else {
            return addReminder(message, chat, user, textMessage);
        }
    }

    private EditMessageText getReminderInfo(Message message, Chat chat, User user, String command) {
        long reminderId;
        try {
            reminderId = Long.parseLong(command.substring(INFO_REMINDER.length() + 1));
        } catch (NumberFormatException e) {
            botStats.incrementErrors();
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        Reminder reminder = reminderService.get(chat, user, reminderId);
        if (reminder == null) {
            botStats.incrementErrors();
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(message.getChatId().toString());
        editMessage.setMessageId(message.getMessageId());
        editMessage.enableHtml(true);
        editMessage.setText(prepareReminderInfoText(reminder));
        editMessage.setReplyMarkup(prepareKeyboardWithReminderInfo(reminder.getId()));

        return editMessage;
    }

    private PartialBotApiMethod<?> addReminder(Message message, Chat chat, User user, String command) {
        log.debug("Request to add new reminder: {}", command);

        String reminderText;
        LocalDate reminderDate = null;
        LocalTime reminderTime = null;

        String datePatternString = "(\\d{2})\\.(\\d{2})\\.(\\d{4})";
        Pattern fullDatePattern = Pattern.compile(datePatternString);
        Matcher matcher = fullDatePattern.matcher(command);
        if (matcher.find()) {
            reminderDate = getDateFromText(command.substring(matcher.start(), matcher.end()));
        } else {
            datePatternString = "(\\d{2})\\.(\\d{2})";
            Pattern shortDatePattern = Pattern.compile(datePatternString);
            matcher = shortDatePattern.matcher(command);
            if (matcher.find()) {
                try {
                    reminderDate = getDateFromText(command.substring(matcher.start(), matcher.end()));
                } catch (Exception ignored) {
                }
            }
        }

        String timePatternString = "(\\d{2}):(\\d{2})";
        Pattern timePattern = Pattern.compile(timePatternString);
        matcher = timePattern.matcher(command);
        if (matcher.find()) {
            reminderTime = getTimeFromText(command.substring(matcher.start(), matcher.end()));
        }

        if (reminderDate != null) {
            command = command.replaceAll(datePatternString, "").trim();
        } else {
            LocalDate dateNow = LocalDate.now();
            if (reminderTime == null || LocalTime.now().isAfter(reminderTime)) {
                reminderDate = dateNow.plusDays(1);
            } else {
                reminderDate = dateNow;
            }
        }

        if (reminderTime != null) {
            command = command.replaceAll(timePatternString, "").trim();
        } else {
            reminderTime = LocalTime.MIN;
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
        sendMessage.enableHtml(true);
        sendMessage.setText(prepareReminderInfoText(reminder));
        sendMessage.setReplyMarkup(prepareKeyboardWithReminderInfo(reminder.getId()));

        return sendMessage;
    }

    private SendMessage manualReminderEdit(Message message, Chat chat, User user, CommandWaiting commandWaiting, String command) {
        if (commandWaiting != null && commandWaiting.getCommandName().equals(this.getClass().getSimpleName().toLowerCase(Locale.ROOT))) {
            commandWaitingService.remove(commandWaiting);
        }
        command = command.replaceAll("\\s+","");

        InlineKeyboardMarkup keyboard;

        Reminder reminder = null;
        String caption = "";
        Pattern idPattern = Pattern.compile("s\\d+");
        Matcher matcher = idPattern.matcher(command);
        if (matcher.find()) {
            try {
                reminder = reminderService.get(
                        chat,
                        user,
                        Long.parseLong(command.substring(matcher.start() + SET_REMINDER.length(), matcher.end())));
            } catch (NumberFormatException ignored) {
            }

            if (reminder != null) {
                matcher = fullDatePattern.matcher(command);
                if (matcher.find()) {
                    reminder.setDate(getDateFromText(command.substring(matcher.start() + 1, matcher.end())));
                } else {
                    matcher = shortDatePattern.matcher(command);
                    if (matcher.find()) {
                        reminder.setDate(getDateFromText(command.substring(matcher.start() + 1, matcher.end()), LocalDate.now().getYear()));
                    } else {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                }

                if (command.indexOf(SET_TIME) < 1) {
                    commandWaitingService.add(chat, user, Remind.class, CALLBACK_COMMAND + SET_REMINDER + reminder.getId() +
                            SET_DATE + dateFormatter.format(reminder.getDate()) + SET_TIME);
                    keyboard = prepareKeyboardWithReminderInfo(prepareButtonRowsWithTimeSettings(reminder), reminder.getId());
                    caption = "Выберите или введите вручную ЧЧ:ММ\n";

                    reminder.setTime(LocalTime.MIN);
                } else {
                    matcher = timePattern.matcher(command);
                    if (matcher.find()) {
                        reminder.setTime(getTimeFromText(command.substring(matcher.start() + 1, matcher.end())));
                        keyboard = prepareKeyboardWithReminderInfo(reminder.getId());
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

        reminderService.save(reminder.setNotified(false));

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.setText(prepareReminderInfoText(reminder) + "<b>" + caption + "</b>");
        sendMessage.setReplyMarkup(keyboard);

        return sendMessage;
    }

    private EditMessageText setReminderByCallback(Message message, Chat chat, User user, String command) {
        Reminder reminder;

        Pattern idPattern = Pattern.compile("s\\d+");
        Matcher matcher = idPattern.matcher(command);
        if (matcher.find()) {
            long reminderId;
            try {
                reminderId = Long.parseLong(command.substring(matcher.start() + SET_REMINDER.length(), matcher.end()));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            reminder = reminderService.get(chat, user, reminderId);
            if (reminder == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        List<List<InlineKeyboardButton>> rowsWithButtons = new ArrayList<>();
        String caption = "";

        matcher = fullDatePattern.matcher(command);
        if (matcher.find()) {
            reminder.setDate(getDateFromText(command.substring(matcher.start() + 1, matcher.end())));
            matcher = timePattern.matcher(command);
            if (matcher.find()) {
                reminder.setTime(getTimeFromText(command.substring(matcher.start() + 1, matcher.end())));
            } else {
                caption = "Выберите или введите вручную ЧЧ:ММ\n";
                rowsWithButtons = prepareButtonRowsWithTimeSettings(reminder);
                commandWaitingService.add(chat, user, Remind.class, CALLBACK_COMMAND + SET_REMINDER + reminder.getId() +
                        SET_DATE + dateFormatter.format(reminder.getDate()) + SET_TIME);
            }
        } else {
            Pattern postponePattern = Pattern.compile(SET_POSTPONE + "(\\d+),(\\d+)");
            matcher = postponePattern.matcher(command);
            if (matcher.find()) {
                PostponeValue postponeValue = new PostponeValue(command.substring(matcher.start() + 1, matcher.end()));

                if (postponeValue.hasMinutes()) {
                    reminder.setTime(postponeValue.getPostponeTime(LocalTime.now()));
                }
                if (postponeValue.hasDays()) {
                    reminder.setDate(postponeValue.getPostponeDate(LocalDate.now()));
                }
            } else {
                caption = "Выберите или введите вручную ДД.ММ или ДД.ММ.ГГГГ\n";
                rowsWithButtons = prepareButtonRowsWithDateSetting(reminder);
                commandWaitingService.add(chat, user, Remind.class, CALLBACK_COMMAND + SET_REMINDER + reminder.getId() +
                        SET_DATE);
            }
        }

        reminderService.save(reminder.setNotified(false));

        InlineKeyboardMarkup keyboard = prepareKeyboardWithReminderInfo(rowsWithButtons, reminder.getId());

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
        LocalDateTime reminderDateTime = reminder.getDate().atTime(reminder.getTime());

        String deltaDates = deltaDatesToString(reminderDateTime, dateTimeNow);
        String leftToRun;
        if (dateTimeNow.isAfter(reminderDateTime)) {
            leftToRun = "Сработало: <b>" + deltaDates + "</b>назад";
        } else {
            leftToRun = "До срабатывания: <b>" + deltaDates + "</b>";
        }

        return "<b>Напоминание</b>\n" +
                reminder.getText() + "\n<i>" +
                formatDate(reminder.getDate()) + " " + formatTime(reminder.getTime()) + "</i>\n" +
                leftToRun;
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

    private InlineKeyboardMarkup prepareKeyboardWithReminderInfo(Long reminderId) {
        return prepareKeyboardWithReminderInfo(new ArrayList<>(), reminderId);
    }

    private InlineKeyboardMarkup prepareKeyboardWithReminderInfo(List<List<InlineKeyboardButton>> rows, Long reminderId) {
        InlineKeyboardButton setReminderButton = new InlineKeyboardButton();
        setReminderButton.setText(Emoji.SETTINGS.getEmoji() + "Настроить");
        setReminderButton.setCallbackData(CALLBACK_SET_REMINDER + reminderId);
        rows.add(List.of(setReminderButton));

        InlineKeyboardButton deleteReminderButton = new InlineKeyboardButton();
        deleteReminderButton.setText(Emoji.DELETE.getEmoji() + "Удалить");
        deleteReminderButton.setCallbackData(CALLBACK_DELETE_COMMAND + reminderId);
        rows.add(List.of(deleteReminderButton));

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.LEFT_ARROW.getEmoji() + "Назад");
        backButton.setCallbackData(CALLBACK_COMMAND);
        rows.add(List.of(backButton));

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

    private PartialBotApiMethod<?> deleteReminderByCallback(Message message, Chat chat, User user, String command) throws BotException {
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
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        try {
            reminderService.remove(reminder);
        } catch (Exception ignored) {}

        return getReminderListWithKeyboard(message, chat, user, FIRST_PAGE, false);
    }

    private PartialBotApiMethod<?> getReminderListWithKeyboard(Message message, Chat chat, User user, int page, boolean newMessage) {
        log.debug("Request to list all reminders for chat {} and user {}, page: {}", chat.getChatId(), user.getUserId(), page);
        Page<Reminder> reminderList = reminderService.getByChatAndUser(chat, user, page);

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setText("<b>Список твоих напоминаний:</b>\n");
            sendMessage.setReplyMarkup(prepareKeyboardWithRemindersForSetting(reminderList, page));

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText("<b>Список твоих напоминаний:</b>\n");
        editMessageText.setReplyMarkup(prepareKeyboardWithRemindersForSetting(reminderList, page));

        return editMessageText;
    }

    private InlineKeyboardMarkup prepareKeyboardWithRemindersForSetting(Page<Reminder> reminderList, int page) {
        final int maxButtonTextLength = 14;

        List<List<InlineKeyboardButton>> rows = reminderList.stream().map(reminder -> {
            List<InlineKeyboardButton> remindersRow = new ArrayList<>();

            String reminderText = reminder.getText();
            if (reminderText.length() > 14) {
                reminderText = reminderText.substring(0, maxButtonTextLength - 3) + "...";
            }

            InlineKeyboardButton reminderButton = new InlineKeyboardButton();
            reminderButton.setText(reminderText);
            reminderButton.setCallbackData(CALLBACK_INFO_REMINDER + " " + reminder.getId());

            remindersRow.add(reminderButton);

            return remindersRow;
        }).collect(Collectors.toList());

        addingMainRows(rows, page, reminderList.getTotalPages());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
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
        String startOfCallbackCommand = CALLBACK_SET_REMINDER + reminderId + SET_POSTPONE;

        InlineKeyboardButton postponeForFiveMinutesButton = new InlineKeyboardButton();
        postponeForFiveMinutesButton.setText("5 минут");
        postponeForFiveMinutesButton.setCallbackData(startOfCallbackCommand + new PostponeValue(5, 0));
        rows.add(List.of(postponeForFiveMinutesButton));

        InlineKeyboardButton postponeForFifteenMinutesButton = new InlineKeyboardButton();
        postponeForFifteenMinutesButton.setText("15 минут");
        postponeForFifteenMinutesButton.setCallbackData(startOfCallbackCommand + new PostponeValue(15, 0));
        rows.add(List.of(postponeForFifteenMinutesButton));

        InlineKeyboardButton postponeForHalfHourButton = new InlineKeyboardButton();
        postponeForHalfHourButton.setText("30 минут");
        postponeForHalfHourButton.setCallbackData(startOfCallbackCommand + new PostponeValue(30, 0));
        rows.add(List.of(postponeForHalfHourButton));

        InlineKeyboardButton postponeForHourButton = new InlineKeyboardButton();
        postponeForHourButton.setText("1 час");
        postponeForHourButton.setCallbackData(startOfCallbackCommand + new PostponeValue(60, 0));
        rows.add(List.of(postponeForHourButton));

        InlineKeyboardButton postponeForThreeHoursButton = new InlineKeyboardButton();
        postponeForThreeHoursButton.setText("3 часа");
        postponeForThreeHoursButton.setCallbackData(startOfCallbackCommand + new PostponeValue(180, 0));
        rows.add(List.of(postponeForThreeHoursButton));

        InlineKeyboardButton postponeForNextDayButton = new InlineKeyboardButton();
        postponeForNextDayButton.setText("Следующий день");
        postponeForNextDayButton.setCallbackData(startOfCallbackCommand + new PostponeValue(0, 1));
        rows.add(List.of(postponeForNextDayButton));

        InlineKeyboardButton setReminderButton = new InlineKeyboardButton();
        setReminderButton.setText(Emoji.SETTINGS.getEmoji() + "Настроить");
        setReminderButton.setCallbackData(CALLBACK_SET_REMINDER + reminderId);
        rows.add(List.of(setReminderButton));

        InlineKeyboardButton deleteReminderButton = new InlineKeyboardButton();
        deleteReminderButton.setText(Emoji.DELETE.getEmoji() + "Удалить");
        deleteReminderButton.setCallbackData(CALLBACK_DELETE_COMMAND + reminderId);
        rows.add(List.of(deleteReminderButton));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    @AllArgsConstructor
    @Value
    private static class PostponeValue {
        long minutes;
        long days;

        public PostponeValue(String values) {
            List<Long> postponeValues = Arrays.stream(values.split("\\s*,\\s*"))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            this.minutes = postponeValues.get(0);
            this.days = postponeValues.get(1);
        }

        public LocalDate getPostponeDate(LocalDate date) {
            return date.plusDays(this.days);
        }

        public LocalTime getPostponeTime(LocalTime time) {
            return time.plusMinutes(this.minutes);
        }

        public boolean hasMinutes() {
            return this.minutes != 0L;
        }

        public boolean hasDays() {
            return this.days != 0;
        }

        @Override
        public String toString() {
            return this.minutes + "," + this.days;
        }
    }
}
