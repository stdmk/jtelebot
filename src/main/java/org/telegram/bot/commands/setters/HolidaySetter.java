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
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;

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
public class HolidaySetter implements Setter<BotResponse> {

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
    public BotResponse set(BotRequest request, String commandText) {
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();
        String lowerCaseCommandText = commandText.toLowerCase(Locale.ROOT);

        if (message.isCallback()) {
            if (emptyHolidayCommands.contains(lowerCaseCommandText) || updateHolidayCommands.contains(lowerCaseCommandText)) {
                return getHolidayListWithKeyboard(message, chat, user, false);
            } else if (containsStartWith(addHolidayCommands, lowerCaseCommandText)) {
                return addHolidayByCallback(message, chat, user, commandText, false);
            } else if (containsStartWith(deleteHolidayCommands, lowerCaseCommandText)) {
                return deleteHolidayByCallback(message, chat, user, commandText);
            }
        }

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

    private EditResponse getKeyboardWithDeletingHolidays(Message message, Chat chat, User user, int page) {
        Page<Holiday> holidayList = holidayService.get(chat, user, page);

        List<List<KeyboardButton>> holidayRows = holidayList.stream().map(holiday -> {
            List<KeyboardButton> holidayRow = new ArrayList<>();

            KeyboardButton holidayButton = new KeyboardButton();
            holidayButton.setName(Emoji.DELETE.getSymbol() + limitStringLength(holiday.getName()));
            holidayButton.setCallback(CALLBACK_DELETE_HOLIDAY_COMMAND + " " + holiday.getId());

            holidayRow.add(holidayButton);

            return holidayRow;
        }).collect(Collectors.toList());

        if (holidayList.getTotalPages() > 1) {
            List<KeyboardButton> pagesRow = new ArrayList<>(2);
            if (page > 0) {
                KeyboardButton backButton = new KeyboardButton();
                backButton.setName(Emoji.LEFT_ARROW.getSymbol() + "Назад");
                backButton.setCallback(CALLBACK_SELECT_PAGE_TV_LIST + (page - 1));

                pagesRow.add(backButton);
            }

            if (page < holidayList.getTotalPages()) {
                KeyboardButton forwardButton = new KeyboardButton();
                forwardButton.setName("Вперёд" + Emoji.RIGHT_ARROW.getSymbol());
                forwardButton.setCallback(CALLBACK_SELECT_PAGE_TV_LIST + (page + 1));

                pagesRow.add(forwardButton);
            }

            holidayRows.add(pagesRow);
        }

        holidayRows.addAll(prepareMainKeyboard());

        return new EditResponse(message)
                .setText("Список добавленных праздников")
                .setKeyboard(new Keyboard(holidayRows))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private BotResponse deleteUserHoliday(Message message, Chat chat, User user, String command) throws BotException {
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

        return buildMessage(message, speechService.getRandomMessageByTag(BotSpeechTag.SAVED), true);
    }

    private EditResponse deleteHolidayByCallback(Message message, Chat chat, User user, String command) throws BotException {
        String deleteHolidayCommand = getLocalizedCommand(command, DELETE_HOLIDAY_COMMAND);
        log.debug("Request to delete city");

        if (command.toLowerCase(Locale.ROOT).equals(deleteHolidayCommand)) {
            return getKeyboardWithDeletingHolidays(message, chat, user, 0);
        }

        String selectPageCommand = getStartsWith(
                internationalizationService.internationalize(SELECT_PAGE_HOLIDAY_LIST),
                command.toLowerCase(Locale.ROOT));
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

    private BotResponse addHolidayByCallback(Message message, Chat chat, User user, String commandText, boolean newMessage) throws BotException {
        String helpText;
        Keyboard keyboard;

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

    private BotResponse addHoliday(Message message, Chat chat, User user, String command) throws BotException {
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

        return buildMessage(message, speechService.getRandomMessageByTag(BotSpeechTag.SAVED), true);
    }

    private BotResponse getHolidayListWithKeyboard(Message message, Chat chat, User user, boolean newMessage) {
        log.debug("Request to list all user tv for chat {} and user {}", chat.getChatId(), user.getUserId());
        List<Holiday> holidayList = holidayService.get(chat, user);

        Keyboard keyboard = new Keyboard(prepareMainKeyboard());
        if (newMessage) {
            return new TextResponse(message)
                    .setText(prepareTextOfUsersHolidays(holidayList))
                    .setKeyboard(keyboard)
                    .setResponseSettings(FormattingStyle.HTML);
        }

        return new EditResponse(message)
                .setText(prepareTextOfUsersHolidays(holidayList))
                .setKeyboard(keyboard)
                .setResponseSettings(FormattingStyle.HTML);
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

    private Keyboard prepareKeyboardWithSelectingYear() {
        return new Keyboard(
                new KeyboardButton()
                        .setName("${setter.holiday.addhelp.withoutyear}")
                        .setCallback(CALLBACK_ADD_HOLIDAY_COMMAND + " 0001"));
    }

    private Keyboard prepareKeyboardWithSelectingMonth(String commandText, Locale locale) {
        List<List<KeyboardButton>> rows = new ArrayList<>();

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

        return new Keyboard(rows);
    }

    private List<KeyboardButton> buildMonthsKeyboardRow(Map<String, String> monthsValues, String commandText) {
        return monthsValues.entrySet().stream().map(entry -> new KeyboardButton()
                .setName(entry.getKey())
                .setCallback(CALLBACK_COMMAND + commandText + "." + entry.getValue()))
                .toList();

    }

    private Keyboard prepareKeyBoardWithSelectingDayOfMonth(String commandText) throws BotException {
        String addHolidayCommand = getLocalizedCommand(commandText, ADD_HOLIDAY_COMMAND);

        List<List<KeyboardButton>> rows = new ArrayList<>();

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
            List<KeyboardButton> daysRow = new ArrayList<>();
            for (int j = i; j < i + 7; j++) {
                if (j > lastDayOfMonth) {
                    break;
                }
                KeyboardButton dayButton = new KeyboardButton();
                dayButton.setName(String.valueOf(j));
                dayButton.setCallback(CALLBACK_COMMAND + commandText + "." + String.format("%02d", j));
                daysRow.add(dayButton);
            }
            rows.add(daysRow);
        }

        return new Keyboard(rows);
    }

    private List<List<KeyboardButton>> prepareMainKeyboard() {
        return List.of(
                List.of(new KeyboardButton()
                        .setName(Emoji.DELETE.getSymbol() + "${setter.holiday.button.delete}")
                        .setCallback(CALLBACK_DELETE_HOLIDAY_COMMAND)),
                List.of(new KeyboardButton()
                        .setName(Emoji.NEW.getSymbol() + "${setter.holiday.button.add}")
                        .setCallback(CALLBACK_ADD_HOLIDAY_COMMAND)),
                List.of(new KeyboardButton()
                        .setName(Emoji.UPDATE.getSymbol() + "${setter.holiday.button.update}")
                        .setCallback(CALLBACK_COMMAND + UPDATE_TV_COMMAND)),
                List.of(new KeyboardButton()
                        .setName(Emoji.BACK.getSymbol() + "${setter.holiday.button.settings}")
                        .setCallback(CALLBACK_COMMAND + "back"))
        );
    }

    private BotResponse buildMessageWithKeyboard(Message message, Keyboard keyboard, String text, boolean newMessage) {
        if (newMessage) {
            return new TextResponse(message)
                    .setText(text)
                    .setKeyboard(keyboard)
                    .setResponseSettings(FormattingStyle.HTML);
        }

        return new EditResponse(message)
                .setText(text)
                .setKeyboard(keyboard)
                .setResponseSettings(FormattingStyle.HTML);
    }

    private BotResponse buildMessage(Message message, String text, boolean newMessage) {
        if (newMessage) {
            return new TextResponse(message)
                    .setText(text)
                    .setResponseSettings(FormattingStyle.HTML);
        }
        return new EditResponse(message)
                .setText(text)
                .setResponseSettings(FormattingStyle.HTML);
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
                text.toLowerCase(Locale.ROOT));

        if (localizedCommand == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        return localizedCommand;
    }

}
