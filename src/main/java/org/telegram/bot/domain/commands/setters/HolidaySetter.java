package org.telegram.bot.domain.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.commands.Set;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.Holiday;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.HolidayService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.cutHtmlTags;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidaySetter implements SetterParent<PartialBotApiMethod<?>> {

    private final ChatService chatService;
    private final UserService userService;
    private final HolidayService holidayService;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;

    private final String CALLBACK_COMMAND = "set ";
    private final String EMPTY_HOLIDAY_COMMAND = "праздник";
    private final String UPDATE_TV_COMMAND = "праздник обновить";
    private final String ADD_HOLIDAY_COMMAND = "праздник добавить";
    private final String CALLBACK_ADD_HOLIDAY_COMMAND = CALLBACK_COMMAND + ADD_HOLIDAY_COMMAND;
    private final String DELETE_HOLIDAY_COMMAND = "праздник удалить";
    private final String CALLBACK_DELETE_HOLIDAY_COMMAND = CALLBACK_COMMAND + DELETE_HOLIDAY_COMMAND;
    private final String SELECT_PAGE_HOLIDAY_LIST = DELETE_HOLIDAY_COMMAND + " page";

    @Override
    public PartialBotApiMethod<?> set(Update update, String commandText) {
        Message message = getMessageFromUpdate(update);
        Chat chat = chatService.get(message.getChatId());
        String lowerCaseCommandText = commandText.toLowerCase();

        if (update.hasCallbackQuery()) {
            User user = userService.get(update.getCallbackQuery().getFrom().getId());

            if (lowerCaseCommandText.equals(EMPTY_HOLIDAY_COMMAND) || lowerCaseCommandText.equals(UPDATE_TV_COMMAND)) {
                return getHolidayListWithKeyboard(message, chat, user, false);
            } else if (lowerCaseCommandText.startsWith(ADD_HOLIDAY_COMMAND)) {
                return addHolidayByCallback(message, chat, user, commandText, false);
            } else if (lowerCaseCommandText.startsWith(DELETE_HOLIDAY_COMMAND)) {
                return deleteHolidayByCallback(message, chat, user, commandText);
            }
        }

        User user = userService.get(message.getFrom().getId());
        if (lowerCaseCommandText.equals(EMPTY_HOLIDAY_COMMAND)) {
            return getHolidayListWithKeyboard(message, chat, user, true);
        } else if (lowerCaseCommandText.startsWith(DELETE_HOLIDAY_COMMAND)) {
            return deleteUserHoliday(message, chat, user, commandText);
        } else if (lowerCaseCommandText.startsWith(ADD_HOLIDAY_COMMAND)) {
            return addHoliday(message, chat, user, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private EditMessageText getKeyboardWithDeletingHolidays(Message message, Chat chat, User user, int page) {

        Page<Holiday> holidayList = holidayService.get(chat, user, page);

        List<List<InlineKeyboardButton>> holidayRows = holidayList.stream().map(holiday -> {
            List<InlineKeyboardButton> holidayRow = new ArrayList<>();

            InlineKeyboardButton holidayButton = new InlineKeyboardButton();
            holidayButton.setText(Emoji.DELETE.getEmoji() + limitStringLength(holiday.getName()));
            holidayButton.setCallbackData(CALLBACK_DELETE_HOLIDAY_COMMAND + " " + holiday.getId());

            holidayRow.add(holidayButton);

            return holidayRow;
        }).collect(Collectors.toList());

        if (holidayList.getTotalPages() > 1) {
            List<InlineKeyboardButton> pagesRow = new ArrayList<>();
            String CALLBACK_SELECT_PAGE_TV_LIST = CALLBACK_COMMAND + SELECT_PAGE_HOLIDAY_LIST;
            if (page > 0) {
                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText(Emoji.LEFT_ARROW.getEmoji() + "Назад");
                backButton.setCallbackData(CALLBACK_SELECT_PAGE_TV_LIST + (page - 1));

                pagesRow.add(backButton);
            }

            if (page < holidayList.getTotalPages()) {
                InlineKeyboardButton forwardButton = new InlineKeyboardButton();
                forwardButton.setText("Вперёд" + Emoji.RIGHT_ARROW.getEmoji());
                forwardButton.setCallbackData(CALLBACK_SELECT_PAGE_TV_LIST + (page + 1));

                pagesRow.add(forwardButton);
            }

            holidayRows.add(pagesRow);
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(addingMainRows(holidayRows));

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableMarkdown(true);
        editMessageText.setText("Список добавленных праздников");
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }

    private SendMessage deleteUserHoliday(Message message, Chat chat, User user, String command) throws BotException {
        log.debug("Request to delete Holiday");

        String params;
        try {
            params = command.substring(DELETE_HOLIDAY_COMMAND.length() + 1);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        Holiday holiday;
        try {
            holiday = holidayService.get(Long.parseLong(params));
        } catch (Exception e) {
            String holidayName = params.toLowerCase(Locale.ROOT);
            holiday = holidayService.get(chat, user, holidayName);

            if (holiday == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }

        if (!holiday.getUser().getUserId().equals(user.getUserId())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER));
        }

        holidayService.remove(holiday);

        return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.SAVED));
    }

    private EditMessageText deleteHolidayByCallback(Message message, Chat chat, User user, String command) throws BotException {
        log.debug("Request to delete city");

        if (command.equals(DELETE_HOLIDAY_COMMAND)) {
            return getKeyboardWithDeletingHolidays(message, chat, user, 0);
        } else if (command.startsWith(SELECT_PAGE_HOLIDAY_LIST)) {
            return getKeyboardWithDeletingHolidays(message, chat, user, Integer.parseInt(command.substring(SELECT_PAGE_HOLIDAY_LIST.length())));
        }

        long holidayId;
        try {
            holidayId = Long.parseLong(command.substring(DELETE_HOLIDAY_COMMAND.length() + 1));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        Holiday holiday = holidayService.get(holidayId);
        if (holiday == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (!holiday.getUser().getUserId().equals(user.getUserId())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER));
        }

        holidayService.remove(holiday);

        return getKeyboardWithDeletingHolidays(message, chat, user, 0);
    }

    private PartialBotApiMethod<?> addHolidayByCallback(Message message, Chat chat, User user, String commandText, boolean newMessage) throws BotException {
        String helpText;
        InlineKeyboardMarkup keyboard;

        if (ADD_HOLIDAY_COMMAND.length() == commandText.length()) {
            helpText = "Напиши год (4 цифры)";
            keyboard = prepareKeyboardWithSelectingYear();
            commandWaitingService.add(chat, user, Set.class, EMPTY_HOLIDAY_COMMAND + " " + commandText);
        } else if (ADD_HOLIDAY_COMMAND.length() + 5 == commandText.length()) {
            helpText = "Выбери месяц";
            keyboard = prepareKeyboardWithSelectingMonth(commandText);
            commandWaitingService.add(chat, user, Set.class, commandText);
        } else if (ADD_HOLIDAY_COMMAND.length() + 8 == commandText.length()) {
            helpText = "Выбери день месяца";
            keyboard = prepareKeyBoardWithSelectingDayOfMonth(commandText);
            commandWaitingService.add(chat, user, Set.class, commandText);
        } else if (ADD_HOLIDAY_COMMAND.length() + 11 == commandText.length()) {
            helpText = "Теперь напиши мне название праздника";
            commandWaitingService.add(chat, user, Set.class, EMPTY_HOLIDAY_COMMAND + " " + commandText);
            return buildMessage(message, helpText, newMessage);
        } else {
            commandWaitingService.remove(commandWaitingService.get(chat, user));
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return buildMessageWithKeyboard(message, keyboard, helpText, newMessage);
    }

    private PartialBotApiMethod<?> addHoliday(Message message, Chat chat, User user, String command) throws BotException {
        log.debug("Request to add new holiday");

        CommandWaiting commandWaiting = commandWaitingService.get(chat, user);

        if (command.length() < ADD_HOLIDAY_COMMAND.length() + 11) {
            return addHolidayByCallback(message, chat, user, command, true);
        }

        String params = command.substring(ADD_HOLIDAY_COMMAND.length() + 1);

        int i = params.indexOf(" ");
        if (i < 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        LocalDate dateOfHoliday;
        String nameOfHoliday;
        try {
            dateOfHoliday = LocalDate.parse(params.substring(0, i), dateFormatter);
            nameOfHoliday = cutHtmlTags(params.substring(i + 1));
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (nameOfHoliday.length() > 55) {
            throw new BotException("Имя праздника слишком длинное. 55 символов максимум.");
        }

        Holiday holiday = holidayService.get(chat, user, nameOfHoliday);
        if (holiday != null) {
            throw new BotException("Такой праздник уже добавлен");
        }

        holiday = new Holiday();
        holiday.setChat(chat);
        holiday.setUser(user);
        holiday.setDate(dateOfHoliday);
        holiday.setName(nameOfHoliday);

        holidayService.save(holiday);

        if (commandWaiting != null && commandWaiting.getCommandName().equals("set")) {
            commandWaitingService.remove(commandWaiting);
        }

        return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.SAVED));
    }

    private PartialBotApiMethod<?> getHolidayListWithKeyboard(Message message, Chat chat, User user, Boolean newMessage) {
        log.debug("Request to list all user tv for chat {} and user {}", chat.getChatId(), user.getUserId());
        List<Holiday> holidayList = holidayService.get(chat, user);

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setText(prepareTextOfUsersHolidays(holidayList));
            sendMessage.setReplyMarkup(prepareKeyboardWithUserHolidayList());

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(prepareTextOfUsersHolidays(holidayList));
        editMessageText.setReplyMarkup(prepareKeyboardWithUserHolidayList());

        return editMessageText;
    }

    private String prepareTextOfUsersHolidays(List<Holiday> userHolidaysList) {
        final StringBuilder buf = new StringBuilder();
        buf.append("<b>Список добавленных тобой праздников:</b>\n");

        userHolidaysList.forEach(holiday ->
                buf.append(holiday.getId()).append(". (")
                    .append(holiday.getDate()).append(") ")
                    .append(limitStringLength(holiday.getName())).append("\n"));

        return buf.toString();
    }

    private InlineKeyboardMarkup prepareKeyboardWithSelectingYear() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> skipAddingYear = new ArrayList<>();
        InlineKeyboardButton skipAdditingYearButton = new InlineKeyboardButton();
        skipAdditingYearButton.setText("Без года");
        skipAdditingYearButton.setCallbackData(CALLBACK_ADD_HOLIDAY_COMMAND + " 0001");
        skipAddingYear.add(skipAdditingYearButton);

        rows.add(skipAddingYear);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup prepareKeyboardWithSelectingMonth(String commandText) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        Map<String, String> monthsValues1 = new LinkedHashMap<>();
        monthsValues1.put("янв", "01");
        monthsValues1.put("фев", "02");
        monthsValues1.put("мар", "03");
        monthsValues1.put("апр", "04");
        monthsValues1.put("май", "05");
        monthsValues1.put("июн", "06");

        Map<String, String> monthsValues2 = new LinkedHashMap<>();
        monthsValues2.put("июл", "07");
        monthsValues2.put("авг", "08");
        monthsValues2.put("сен", "09");
        monthsValues2.put("окт", "10");
        monthsValues2.put("ноя", "11");
        monthsValues2.put("дек", "12");

        rows.add(buildMonthsKeyboardRow(monthsValues1, commandText));
        rows.add(buildMonthsKeyboardRow(monthsValues2, commandText));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private List<InlineKeyboardButton> buildMonthsKeyboardRow(Map<String, String> monthsValues, String commandText) {
        List<InlineKeyboardButton> monthsRow = new ArrayList<>();

        monthsValues.forEach((key, value) -> {
            InlineKeyboardButton monthButton = new InlineKeyboardButton();
            monthButton.setText(key);
            monthButton.setCallbackData(CALLBACK_COMMAND + commandText + "." + value);
            monthsRow.add(monthButton);
        });

        return monthsRow;
    }

    private InlineKeyboardMarkup prepareKeyBoardWithSelectingDayOfMonth(String commandText) throws BotException {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String date = commandText.substring(ADD_HOLIDAY_COMMAND.length() + 1);
        YearMonth yearMonth;
        try {
            int year = Integer.parseInt(date.substring(0, 4));
            int month = Integer.parseInt(date.substring(5));
            yearMonth = YearMonth.of(year, month);
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        int lastDayOfMonth = yearMonth.lengthOfMonth();
        for (int i = 1; i < lastDayOfMonth; i = i + 7) {
            List<InlineKeyboardButton> daysRow = new ArrayList<>();
            for (int j = i; j < i + 7; j++) {
                if (j > lastDayOfMonth) {
                    break;
                }
                InlineKeyboardButton dayButton = new InlineKeyboardButton();
                dayButton.setText(String.valueOf(j));
                dayButton.setCallbackData(CALLBACK_COMMAND + commandText + "." + String.format("%02d", j));
                daysRow.add(dayButton);
            }
            rows.add(daysRow);
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup prepareKeyboardWithUserHolidayList() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        addingMainRows(rows);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private List<List<InlineKeyboardButton>> addingMainRows(List<List<InlineKeyboardButton>> rows) {
        List<InlineKeyboardButton> deleteButtonRow = new ArrayList<>();
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText(Emoji.DELETE.getEmoji() + "Удалить");
        deleteButton.setCallbackData(CALLBACK_DELETE_HOLIDAY_COMMAND);
        deleteButtonRow.add(deleteButton);

        List<InlineKeyboardButton> selectButtonRow = new ArrayList<>();
        InlineKeyboardButton selectButton = new InlineKeyboardButton();
        selectButton.setText(Emoji.NEW.getEmoji() + "Добавить");
        selectButton.setCallbackData(CALLBACK_ADD_HOLIDAY_COMMAND);
        selectButtonRow.add(selectButton);

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getEmoji() + "Обновить");
        updateButton.setCallbackData(CALLBACK_COMMAND + UPDATE_TV_COMMAND);
        updateButtonRow.add(updateButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getEmoji() + "Установки");
        backButton.setCallbackData(CALLBACK_COMMAND + "back");
        backButtonRow.add(backButton);

        rows.add(deleteButtonRow);
        rows.add(selectButtonRow);
        rows.add(updateButtonRow);
        rows.add(backButtonRow);

        return rows;
    }

    private PartialBotApiMethod<?> buildMessageWithKeyboard(Message message, InlineKeyboardMarkup keyboard, String text, Boolean newMessage) {
        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setText(text);
            sendMessage.setReplyMarkup(keyboard);

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(text);
        editMessageText.setReplyMarkup(keyboard);

        return editMessageText;
    }

    private PartialBotApiMethod<?> buildMessage(Message message, String text, Boolean newMessage) {
        if (newMessage) {
            buildSendMessageWithText(message, text);
        }
        return buildEditMessageWithText(message, text);
    }

    private SendMessage buildSendMessageWithText(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(text);

        return sendMessage;
    }

    private EditMessageText buildEditMessageWithText(Message message, String text) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableMarkdown(true);
        editMessageText.setText(text);

        return editMessageText;
    }

    private String limitStringLength(String name) {
        final int MAX_NAME_LENGTH = 16;
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH) + "...";
        }

        return name;
    }
}
