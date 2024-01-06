package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.telegram.bot.commands.Set;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.Holiday;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidaySetter implements Setter<PartialBotApiMethod<?>> {

    private static final Integer MAX_HOLIDAYS_NAME_LENGTH = 55;
    private static final int MAX_HOLIDAY_NAME_STRING_LENGTH = 16;

    private static final String CALLBACK_COMMAND = "set ";
    private static final String EMPTY_HOLIDAY_COMMAND = "${setter.holiday.emptycommand}";
    private static final String UPDATE_TV_COMMAND = EMPTY_HOLIDAY_COMMAND + " ${setter.holiday.update}";
    private static final String ADD_HOLIDAY_COMMAND = EMPTY_HOLIDAY_COMMAND + " ${setter.holiday.add}";
    private static final String CALLBACK_ADD_HOLIDAY_COMMAND = CALLBACK_COMMAND + ADD_HOLIDAY_COMMAND;
    private static final String DELETE_HOLIDAY_COMMAND = EMPTY_HOLIDAY_COMMAND + " ${setter.holiday.remove}";
    private static final String CALLBACK_DELETE_HOLIDAY_COMMAND = CALLBACK_COMMAND + DELETE_HOLIDAY_COMMAND;
    private static final String SELECT_PAGE_HOLIDAY_LIST = DELETE_HOLIDAY_COMMAND + " page";
    private static final String CALLBACK_SELECT_PAGE_TV_LIST = CALLBACK_COMMAND + SELECT_PAGE_HOLIDAY_LIST;

    private final java.util.Set<String> emptyHolidayCommands = new HashSet<>();
    private final java.util.Set<String> updateHolidayCommands = new HashSet<>();
    private final java.util.Set<String> addHolidayCommands = new HashSet<>();
    private final java.util.Set<String> deleteHolidayCommands = new HashSet<>();

    private final HolidayService holidayService;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final InternationalizationService internationalizationService;
    private final LanguageResolver languageResolver;

    @PostConstruct
    private void postConstruct() {
        emptyHolidayCommands.addAll(internationalizationService.getAllTranslations("setter.holiday.emptycommand"));
        updateHolidayCommands.addAll(internationalizationService.internationalize(UPDATE_TV_COMMAND));
        addHolidayCommands.addAll(internationalizationService.internationalize(ADD_HOLIDAY_COMMAND));
        deleteHolidayCommands.addAll(internationalizationService.internationalize(DELETE_HOLIDAY_COMMAND));
    }

    @Override
    public boolean canProcessed(String command) {
        return emptyHolidayCommands.stream().anyMatch(command::startsWith);
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.TRUSTED;
    }

    @Override
    public PartialBotApiMethod<?> set(Update update, String commandText) {
        Message message = getMessageFromUpdate(update);
        Chat chat = new Chat().setChatId(message.getChatId());
        String lowerCaseCommandText = commandText.toLowerCase();

        if (update.hasCallbackQuery()) {
            User user = new User().setUserId(update.getCallbackQuery().getFrom().getId());

            if (emptyHolidayCommands.contains(lowerCaseCommandText) || updateHolidayCommands.contains(lowerCaseCommandText)) {
                return getHolidayListWithKeyboard(message, chat, user, false);
            } else if (containsStartWith(addHolidayCommands, lowerCaseCommandText)) {
                return addHolidayByCallback(message, chat, user, commandText, false);
            } else if (containsStartWith(deleteHolidayCommands, lowerCaseCommandText)) {
                return deleteHolidayByCallback(message, chat, user, commandText);
            }
        }

        User user = new User().setUserId(message.getFrom().getId());
        if (emptyHolidayCommands.contains(lowerCaseCommandText)) {
            return getHolidayListWithKeyboard(message, chat, user, true);
        } else if (containsStartWith(deleteHolidayCommands, lowerCaseCommandText)) {
            return deleteUserHoliday(message, chat, user, commandText);
        } else if (containsStartWith(addHolidayCommands, lowerCaseCommandText)) {
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
            holidayButton.setText(Emoji.DELETE.getSymbol() + limitStringLength(holiday.getName()));
            holidayButton.setCallbackData(CALLBACK_DELETE_HOLIDAY_COMMAND + " " + holiday.getId());

            holidayRow.add(holidayButton);

            return holidayRow;
        }).collect(Collectors.toList());

        if (holidayList.getTotalPages() > 1) {
            List<InlineKeyboardButton> pagesRow = new ArrayList<>();
            if (page > 0) {
                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText(Emoji.LEFT_ARROW.getSymbol() + "Назад");
                backButton.setCallbackData(CALLBACK_SELECT_PAGE_TV_LIST + (page - 1));

                pagesRow.add(backButton);
            }

            if (page < holidayList.getTotalPages()) {
                InlineKeyboardButton forwardButton = new InlineKeyboardButton();
                forwardButton.setText("Вперёд" + Emoji.RIGHT_ARROW.getSymbol());
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
        String deleteHolidayCommand = getLocalizedCommand(command, DELETE_HOLIDAY_COMMAND);
        log.debug("Request to delete Holiday");

        String params;
        try {
            params = command.substring(deleteHolidayCommand.length() + 1);
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
        String deleteHolidayCommand = getLocalizedCommand(command, DELETE_HOLIDAY_COMMAND);
        log.debug("Request to delete city");

        if (command.toLowerCase().equals(deleteHolidayCommand)) {
            return getKeyboardWithDeletingHolidays(message, chat, user, 0);
        }

        String selectPageCommand = getStartsWith(
                internationalizationService.internationalize(SELECT_PAGE_HOLIDAY_LIST),
                command.toLowerCase());
        if (selectPageCommand != null && command.startsWith(SELECT_PAGE_HOLIDAY_LIST)) {
            return getKeyboardWithDeletingHolidays(message, chat, user, Integer.parseInt(command.substring(SELECT_PAGE_HOLIDAY_LIST.length())));
        }

        long holidayId;
        try {
            holidayId = Long.parseLong(command.substring(deleteHolidayCommand.length() + 1));
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

        String addHolidayCommand = getLocalizedCommand(commandText, ADD_HOLIDAY_COMMAND);

        if (addHolidayCommand.length() == commandText.length()) {
            helpText = "${setter.holiday.addhelp.year}";
            keyboard = prepareKeyboardWithSelectingYear();
            commandWaitingService.add(chat, user, Set.class, EMPTY_HOLIDAY_COMMAND + " " + commandText);
        } else if (addHolidayCommand.length() + 5 == commandText.length()) {
            helpText = "${setter.holiday.addhelp.choisemonth}";
            keyboard = prepareKeyboardWithSelectingMonth(commandText, languageResolver.getLocale(message, user));
            commandWaitingService.add(chat, user, Set.class, commandText);
        } else if (addHolidayCommand.length() + 8 == commandText.length()) {
            helpText = "${setter.holiday.addhelp.choisedayofmonth}";
            keyboard = prepareKeyBoardWithSelectingDayOfMonth(commandText);
            commandWaitingService.add(chat, user, Set.class, commandText);
        } else if (addHolidayCommand.length() + 11 == commandText.length()) {
            helpText = "${setter.holiday.addhelp.holidayname}";
            commandWaitingService.add(chat, user, Set.class, EMPTY_HOLIDAY_COMMAND + " " + commandText);
            return buildMessage(message, helpText, newMessage);
        } else {
            commandWaitingService.remove(commandWaitingService.get(chat, user));
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return buildMessageWithKeyboard(message, keyboard, helpText, newMessage);
    }

    private PartialBotApiMethod<?> addHoliday(Message message, Chat chat, User user, String command) throws BotException {
        String addHolidayCommand = getLocalizedCommand(command, ADD_HOLIDAY_COMMAND);
        log.debug("Request to add new holiday");

        CommandWaiting commandWaiting = commandWaitingService.get(chat, user);

        if (command.length() < addHolidayCommand.length() + 11) {
            return addHolidayByCallback(message, chat, user, command, true);
        }

        String params = command.substring(addHolidayCommand.length() + 1);

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

        if (nameOfHoliday.length() > MAX_HOLIDAYS_NAME_LENGTH) {
            throw new BotException("${setter.holiday.addhelp.toolongname}. " + MAX_HOLIDAYS_NAME_LENGTH + " ${setter.holiday.addhelp.maxlength}.");
        }

        Holiday holiday = holidayService.get(chat, user, nameOfHoliday);
        if (holiday != null) {
            throw new BotException("${setter.holiday.addhelp.alreadyexists}");
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

    private PartialBotApiMethod<?> getHolidayListWithKeyboard(Message message, Chat chat, User user, boolean newMessage) {
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
        buf.append("<b>${setter.holiday.caption}:</b>\n");

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
        skipAdditingYearButton.setText("${setter.holiday.addhelp.withoutyear}");
        skipAdditingYearButton.setCallbackData(CALLBACK_ADD_HOLIDAY_COMMAND + " 0001");
        skipAddingYear.add(skipAdditingYearButton);

        rows.add(skipAddingYear);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup prepareKeyboardWithSelectingMonth(String commandText, Locale locale) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        Map<String, String> monthsValues1 = new LinkedHashMap<>();
        monthsValues1.put(Month.JANUARY.getDisplayName(TextStyle.SHORT, locale), "01");
        monthsValues1.put(Month.FEBRUARY.getDisplayName(TextStyle.SHORT, locale), "02");
        monthsValues1.put(Month.MARCH.getDisplayName(TextStyle.SHORT, locale), "03");
        monthsValues1.put(Month.APRIL.getDisplayName(TextStyle.SHORT, locale), "04");
        monthsValues1.put(Month.MAY.getDisplayName(TextStyle.SHORT, locale), "05");
        monthsValues1.put(Month.JUNE.getDisplayName(TextStyle.SHORT, locale), "06");

        Map<String, String> monthsValues2 = new LinkedHashMap<>();
        monthsValues2.put(Month.JULY.getDisplayName(TextStyle.SHORT, locale), "07");
        monthsValues2.put(Month.AUGUST.getDisplayName(TextStyle.SHORT, locale), "08");
        monthsValues2.put(Month.SEPTEMBER.getDisplayName(TextStyle.SHORT, locale), "09");
        monthsValues2.put(Month.OCTOBER.getDisplayName(TextStyle.SHORT, locale), "10");
        monthsValues2.put(Month.NOVEMBER.getDisplayName(TextStyle.SHORT, locale), "11");
        monthsValues2.put(Month.DECEMBER.getDisplayName(TextStyle.SHORT, locale), "12");

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
        String addHolidayCommand = getLocalizedCommand(commandText, ADD_HOLIDAY_COMMAND);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String date = commandText.substring(addHolidayCommand.length() + 1);
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
        deleteButton.setText(Emoji.DELETE.getSymbol() + "${setter.holiday.button.delete}");
        deleteButton.setCallbackData(CALLBACK_DELETE_HOLIDAY_COMMAND);
        deleteButtonRow.add(deleteButton);

        List<InlineKeyboardButton> selectButtonRow = new ArrayList<>();
        InlineKeyboardButton selectButton = new InlineKeyboardButton();
        selectButton.setText(Emoji.NEW.getSymbol() + "${setter.holiday.button.add}");
        selectButton.setCallbackData(CALLBACK_ADD_HOLIDAY_COMMAND);
        selectButtonRow.add(selectButton);

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getSymbol() + "${setter.holiday.button.update}");
        updateButton.setCallbackData(CALLBACK_COMMAND + UPDATE_TV_COMMAND);
        updateButtonRow.add(updateButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getSymbol() + "${setter.holiday.button.settings}");
        backButton.setCallbackData(CALLBACK_COMMAND + "back");
        backButtonRow.add(backButton);

        rows.add(deleteButtonRow);
        rows.add(selectButtonRow);
        rows.add(updateButtonRow);
        rows.add(backButtonRow);

        return rows;
    }

    private PartialBotApiMethod<?> buildMessageWithKeyboard(Message message, InlineKeyboardMarkup keyboard, String text, boolean newMessage) {
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

    private PartialBotApiMethod<?> buildMessage(Message message, String text, boolean newMessage) {
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
        if (name.length() > MAX_HOLIDAY_NAME_STRING_LENGTH) {
            name = name.substring(0, MAX_HOLIDAY_NAME_STRING_LENGTH) + "...";
        }

        return name;
    }

    private String getLocalizedCommand(String text, String command) {
        String localizedCommand = getStartsWith(
                internationalizationService.internationalize(command),
                text.toLowerCase());

        if (localizedCommand == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        return localizedCommand;
    }

}
