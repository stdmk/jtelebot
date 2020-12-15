package org.telegram.bot.domain.commands.setters;

import liquibase.pro.packaged.E;
import liquibase.pro.packaged.I;
import liquibase.pro.packaged.S;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.domain.enums.ParseModes;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import javax.transaction.Transactional;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.TimeZones;

@Service
@AllArgsConstructor
public class CitySetter implements SetterParent<PartialBotApiMethod<?>> {

    private final Logger log = LoggerFactory.getLogger(CitySetter.class);

    private final CityService cityService;
    private final UserCityService userCityService;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final UserService userService;

    private final String CALLBACK_COMMAND = "установить ";
    private final String UPDATE_CITY_COMMAND = "город обновить";
    private final String SELECT_CITY_COMMAND = "город выбрать";
    private final String CALLBACK_SELECT_CITY_COMMAND = CALLBACK_COMMAND + SELECT_CITY_COMMAND;
    private final String DELETE_CITY_COMMAND = "город удалить";
    private final String CALLBACK_DELETE_CITY_COMMAND = CALLBACK_COMMAND + DELETE_CITY_COMMAND;
    private final String ADD_CITY_COMMAND = "город добавить";
    private final String CALLBACK_ADD_CITY_COMMAND = CALLBACK_COMMAND + ADD_CITY_COMMAND;
    private final String SET_TIMEZONE = "город зона";
    private final String CALLBACK_SET_TIMEZONE = CALLBACK_COMMAND + SET_TIMEZONE;

    @Override
    public PartialBotApiMethod<?> set(Update update, String commandText) throws Exception {
        Message message = getMessageFromUpdate(update);
        String lowerCaseCommandText = commandText.toLowerCase();

        String EMPTY_CITY_COMMAND = "город";
        if (update.hasCallbackQuery()) {
            if (lowerCaseCommandText.equals(EMPTY_CITY_COMMAND) || lowerCaseCommandText.equals(UPDATE_CITY_COMMAND)) {
                return getMainKeyboard(message, update.getCallbackQuery().getFrom().getId(), false);
            } else if (lowerCaseCommandText.startsWith(SELECT_CITY_COMMAND)) {
                return selectCityByCallback(message, commandText, update.getCallbackQuery().getFrom().getId());
            } else if (lowerCaseCommandText.startsWith(DELETE_CITY_COMMAND)) {
                return deleteCityByCallback(message, commandText, update.getCallbackQuery().getFrom().getId());
            } else if (lowerCaseCommandText.startsWith(ADD_CITY_COMMAND)) {
                return addCityByCallback(message, update.getCallbackQuery().getFrom().getId(), false);
            } else if (lowerCaseCommandText.startsWith(SET_TIMEZONE)) {
                return setTimeZoneForCity(message, commandText, update.getCallbackQuery().getFrom().getId());
            }
        }

        if (lowerCaseCommandText.equals(EMPTY_CITY_COMMAND)) {
            return getMainKeyboard(message, message.getFrom().getId(), true);
        } else if (lowerCaseCommandText.startsWith(DELETE_CITY_COMMAND)) {
            return deleteCity(message, commandText);
        } else if (lowerCaseCommandText.startsWith(ADD_CITY_COMMAND)) {
            return addCity(message, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }
    }

    private EditMessageText selectCityByCallback(Message message, String command, Integer userId) throws BotException {
        if (command.equals(SELECT_CITY_COMMAND)) {
            return getKeyboardWithCities(message, CALLBACK_SELECT_CITY_COMMAND, userId);
        }

        long cityId;
        try {
            cityId = Long.parseLong(command.substring(SELECT_CITY_COMMAND.length() + 1));
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        City city = cityService.get(cityId);
        if (city == null) {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        User user = userService.get(userId);
        Long chatId = message.getChatId();
        UserCity userCity = userCityService.get(user, chatId);
        if (userCity == null) {
            userCity = new UserCity();
            userCity.setChatId(chatId);
            userCity.setUser(user);
        }
        userCity.setCity(city);

        userCityService.save(userCity);

        return (EditMessageText) getMainKeyboard(message, user.getUserId(), false);
    }

    private PartialBotApiMethod<?> addCity(Message message, String command) throws BotException {
        log.debug("Request to add new city");
        if (command.equals(ADD_CITY_COMMAND)) {
            return addCityByCallback(message, message.getFrom().getId(), true);
        }

        String params = command.substring(ADD_CITY_COMMAND.length() + 1);

        int i = params.indexOf(" ");
        if (i < 0) {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        City newCity = new City();
        newCity.setNameRu(params.substring(0, i));
        newCity.setNameEn(params.substring(i + 1));
        newCity.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()).getID());
        newCity.setUser(userService.get(message.getFrom().getId()));
        cityService.save(newCity);

        CommandWaiting commandWaiting = commandWaitingService.get(message.getChatId(), message.getFrom().getId());
        if (commandWaiting != null && commandWaiting.getCommandName().equals("set")) {
            commandWaitingService.remove(commandWaiting);
        }

        return getKeyboardWithTimeZones(message, newCity.getId());
    }

    private SendMessage deleteCity(Message message, String command) throws BotException {
        log.debug("Request to delete city");

        String params;
        try {
            params = command.substring(DELETE_CITY_COMMAND.length() + 1);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        long cityId;

        try {
            cityId = Long.parseLong(params);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        City city = cityService.get(cityId);
        if (!city.getUser().equals(userService.get(message.getFrom().getId()))) {
            throw new BotException(speechService.getRandomMessageByTag("notOwner"));
        }

        cityService.remove(city);

        return buildSendMessageWithText(message, speechService.getRandomMessageByTag("saved"));
    }

    @Transactional
    private EditMessageText setTimeZoneForCity(Message message, String command, Integer userId) throws BotException {
        log.debug("Request to set timezone for city");

        String params;
        try {
            params = command.substring(SET_TIMEZONE.length() + 1);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        long cityId;
        TimeZone timeZone;

        int i = params.indexOf(" ");
        if (i < 0) {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        try {
            cityId = Long.parseLong(params.substring(0, i));
            timeZone = TimeZone.getTimeZone(params.substring(i + 1));
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        City city = cityService.get(cityId);
        if (!city.getUser().getUserId().equals(userId)) {
            throw new BotException(speechService.getRandomMessageByTag("notOwner"));
        }

        city.setTimeZone(timeZone.getID());
        cityService.save(city);

        return (EditMessageText) getMainKeyboard(message, userId, false);
    }

    private PartialBotApiMethod<?> addCityByCallback(Message message, Integer userId, boolean newMessage) {
        log.debug("Empty params. Waiting to continue...");
        CommandWaiting commandWaiting = commandWaitingService.get(message.getChatId(), userId);
        if (commandWaiting == null) {
            commandWaiting = new CommandWaiting();
            commandWaiting.setChatId(message.getChatId());
            commandWaiting.setUserId(userId);
        }
        commandWaiting.setCommandName("set");
        commandWaiting.setIsFinished(false);
        commandWaiting.setTextMessage(CALLBACK_ADD_CITY_COMMAND + " ");
        commandWaitingService.save(commandWaiting);

        String ADDING_HELP_TEXT_NAMES = "\nНапиши мне через пробел название города на русском и английском языках\nНапример: Тверь Tver";
        if (newMessage) {
            return buildSendMessageWithText(message, ADDING_HELP_TEXT_NAMES);
        }

        return buildEditMessageWithText(message, ADDING_HELP_TEXT_NAMES);
    }

    private EditMessageText deleteCityByCallback(Message message, String command, Integer userId) throws BotException {
        log.debug("Request to delete city");

        if (command.equals(DELETE_CITY_COMMAND)) {
            return getKeyboardWithCities(message, CALLBACK_DELETE_CITY_COMMAND, userId);
        }

        long cityId;
        try {
            cityId = Long.parseLong(command.substring(DELETE_CITY_COMMAND.length() + 1));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        City city = cityService.get(cityId);
        if (city == null) {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        List<UserCity> userCities = userCityService.getAll(city);
        if (!userCities.isEmpty()) {
            throw new BotException(speechService.getRandomMessageByTag("databaseIntegrity"));
        }

        try {
            cityService.remove(city);
        } catch (Exception ignored) {}

        return getKeyboardWithCities(message, CALLBACK_DELETE_CITY_COMMAND, userId);
    }

    private EditMessageText getKeyboardWithCities(Message message, String callbackCommand, Integer userId) {
        String emoji;
        String title;
        User user = userService.get(userId);
        List<City> cities;

        if (callbackCommand.equals(CALLBACK_DELETE_CITY_COMMAND)) {
            title = "Список добавленных тобой городов";
            emoji = Emoji.DELETE.getEmoji();
            cities = cityService.getAll(user);
        } else {
            title = "Выбери город из списка или добавь новый";
            emoji = "";
            cities = cityService.getAll();
        }

        List<List<InlineKeyboardButton>> cityRows = cities.stream().map(city -> {
            List<InlineKeyboardButton> cityRow = new ArrayList<>();

            InlineKeyboardButton cityButton = new InlineKeyboardButton();
            cityButton.setText(emoji + city.getNameRu());
            cityButton.setCallbackData(callbackCommand + " " + city.getId());

            cityRow.add(cityButton);

            return cityRow;
        }).collect(Collectors.toList());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(addingMainRows(cityRows));

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableMarkdown(true);
        editMessageText.setText(title);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }

    private PartialBotApiMethod<?> getMainKeyboard(Message message, Integer userId, boolean newMessage) {
        User user = userService.get(userId);
        Long chatId = message.getChatId();
        String responseText;

        UserCity userCity = userCityService.get(user, chatId);
        if (userCity == null || userCity.getCity() == null) {
            responseText = "Город не установлен. Нажми кнопку \"Выбрать\"";
        } else {
            responseText = "Выбранный город: " + userCity.getCity().getNameRu() + " (" + userCity.getCity().getTimeZone() + ")";
        }

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableMarkdown(true);
            sendMessage.setText(responseText);
            sendMessage.setReplyMarkup(prepareMainKeyboard());

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableMarkdown(true);
        editMessageText.setText(responseText);
        editMessageText.setReplyMarkup(prepareMainKeyboard());

        return editMessageText;
    }

    private InlineKeyboardMarkup prepareMainKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(addingMainRows(rows));

        return inlineKeyboardMarkup;
    }

    private List<List<InlineKeyboardButton>> addingMainRows(List<List<InlineKeyboardButton>> rows) {

        List<InlineKeyboardButton> selectButtonRow = new ArrayList<>();
        InlineKeyboardButton selectButton = new InlineKeyboardButton();
        selectButton.setText(Emoji.RIGHT_ARROW_CURVING_UP.getEmoji() + "Выбрать");
        selectButton.setCallbackData(CALLBACK_SELECT_CITY_COMMAND);
        selectButtonRow.add(selectButton);

        List<InlineKeyboardButton> addButtonRow = new ArrayList<>();
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText(Emoji.NEW.getEmoji() + "Добавить");
        addButton.setCallbackData(CALLBACK_ADD_CITY_COMMAND);
        addButtonRow.add(addButton);

        List<InlineKeyboardButton> deleteButtonRow = new ArrayList<>();
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText(Emoji.DELETE.getEmoji() + "Удалить");
        deleteButton.setCallbackData(CALLBACK_DELETE_CITY_COMMAND);
        deleteButtonRow.add(deleteButton);

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getEmoji() + "Обновить");
        updateButton.setCallbackData(CALLBACK_COMMAND + UPDATE_CITY_COMMAND);
        updateButtonRow.add(updateButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getEmoji() + "Установки");
        backButton.setCallbackData(CALLBACK_COMMAND + "back");
        backButtonRow.add(backButton);

        rows.add(selectButtonRow);
        rows.add(addButtonRow);
        rows.add(deleteButtonRow);
        rows.add(updateButtonRow);
        rows.add(backButtonRow);

        return rows;
    }

    private SendMessage getKeyboardWithTimeZones(Message message, Long cityId) {
        List<List<InlineKeyboardButton>> rows = Arrays
                .stream(TimeZones.values())
                .map(zone -> {
                    List<InlineKeyboardButton> zoneRow = new ArrayList<>();
                    InlineKeyboardButton zoneButton = new InlineKeyboardButton();
                    zoneButton.setText(zone.getZone());
                    zoneButton.setCallbackData(CALLBACK_SET_TIMEZONE + " " + cityId + " " + zone.getZone());
                    zoneRow.add(zoneButton);

                    return zoneRow;
                })
                .collect(Collectors.toList());

        String ADDING_HELP_TEXT_TIMEZONE = "\nЧасовой пояс города выставлен по умолчанию.\nПожалуйста, выбери значение из предложенных";

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(ADDING_HELP_TEXT_TIMEZONE);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        return sendMessage;
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
}
