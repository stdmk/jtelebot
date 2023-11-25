package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.commands.Set;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.City;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;
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
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.TimeZones;
import static org.telegram.bot.utils.TextUtils.containsStartWith;
import static org.telegram.bot.utils.TextUtils.getStartsWith;

@Service
@RequiredArgsConstructor
@Slf4j
public class CitySetter implements Setter<PartialBotApiMethod<?>> {

    private static final String CALLBACK_COMMAND = "${setter.command} ";
    private static final String EMPTY_CITY_COMMAND = "${setter.city.emptycommand}";
    private static final String UPDATE_CITY_COMMAND = EMPTY_CITY_COMMAND + " ${setter.city.update}";
    private static final String SELECT_CITY_COMMAND = EMPTY_CITY_COMMAND + " ${setter.city.select}";
    private static final String CALLBACK_SELECT_CITY_COMMAND = CALLBACK_COMMAND + SELECT_CITY_COMMAND;
    private static final String DELETE_CITY_COMMAND = EMPTY_CITY_COMMAND + " ${setter.city.remove}";
    private static final String CALLBACK_DELETE_CITY_COMMAND = CALLBACK_COMMAND + DELETE_CITY_COMMAND;
    private static final String ADD_CITY_COMMAND = EMPTY_CITY_COMMAND + " ${setter.city.add}";
    private static final String CALLBACK_ADD_CITY_COMMAND = CALLBACK_COMMAND + ADD_CITY_COMMAND;
    private static final String SET_TIMEZONE = EMPTY_CITY_COMMAND + " ${setter.city.zone}";
    private static final String CALLBACK_SET_TIMEZONE = CALLBACK_COMMAND + SET_TIMEZONE;
    private static final String ADDING_HELP_TEXT_NAMES = "${setter.city.commandwaitingstart}";
    private static final String ADDING_HELP_TEXT_TIMEZONE = "${setter.city.selectzonehelp}";

    private final java.util.Set<String> emptyCityCommands = new HashSet<>();
    private final java.util.Set<String> updateCityCommands = new HashSet<>();
    private final java.util.Set<String> deleteCityCommands = new HashSet<>();
    private final java.util.Set<String> addCityCommands = new HashSet<>();
    private final java.util.Set<String> selectCityCommands = new HashSet<>();
    private final java.util.Set<String> setTimeZoneCommands = new HashSet<>();

    private final CityService cityService;
    private final UserCityService userCityService;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final InternationalizationService internationalizationService;

    @PostConstruct
    private void postConstruct() {
        emptyCityCommands.addAll(internationalizationService.getAllTranslations("setter.city.emptycommand"));
        updateCityCommands.addAll(internationalizationService.internationalize(UPDATE_CITY_COMMAND));
        deleteCityCommands.addAll(internationalizationService.internationalize(DELETE_CITY_COMMAND));
        addCityCommands.addAll(internationalizationService.internationalize(ADD_CITY_COMMAND));
        selectCityCommands.addAll(internationalizationService.internationalize(SELECT_CITY_COMMAND));
        setTimeZoneCommands.addAll(internationalizationService.internationalize(SET_TIMEZONE));
    }

    @Override
    public boolean canProcessed(String command) {
        return emptyCityCommands.stream().anyMatch(command::startsWith);
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

            if (emptyCityCommands.contains(lowerCaseCommandText) || updateCityCommands.contains(lowerCaseCommandText)) {
                return getMainKeyboard(message, chat, user, false);
            } else if (containsStartWith(selectCityCommands, lowerCaseCommandText)) {
                return selectCityByCallback(message, chat, user, commandText);
            } else if (containsStartWith(deleteCityCommands, lowerCaseCommandText)) {
                return deleteCityByCallback(message, user, commandText);
            } else if (containsStartWith(addCityCommands, lowerCaseCommandText)) {
                return addCityByCallback(message,  chat, user, false);
            } else if (containsStartWith(setTimeZoneCommands, lowerCaseCommandText)) {
                return setTimeZoneForCity(message, chat, user, commandText);
            }
        }

        User user = new User().setUserId(message.getFrom().getId());
        if (emptyCityCommands.contains(lowerCaseCommandText)) {
            return getMainKeyboard(message,  chat, user, true);
        } else if (containsStartWith(deleteCityCommands, lowerCaseCommandText)) {
            return deleteCity(message, user, commandText);
        } else if (containsStartWith(addCityCommands, lowerCaseCommandText)) {
            return addCity(message, chat, user, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private EditMessageText selectCityByCallback(Message message, Chat chat, User user, String command) throws BotException {
        String selectCityCommand = getLocalizedCommand(command, SELECT_CITY_COMMAND);

        if (command.toLowerCase().equals(selectCityCommand)) {
            return getKeyboardWithCities(message, user, CALLBACK_SELECT_CITY_COMMAND);
        }

        long cityId;
        try {
            cityId = Long.parseLong(command.substring(selectCityCommand.length() + 1));
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        City city = cityService.get(cityId);
        if (city == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        UserCity userCity = userCityService.get(user, chat);
        if (userCity == null) {
            userCity = new UserCity();
            userCity.setChat(chat);
            userCity.setUser(user);
        }
        userCity.setCity(city);

        userCityService.save(userCity);

        return (EditMessageText) getMainKeyboard(message, chat, user, false);
    }

    private PartialBotApiMethod<?> addCity(Message message, Chat chat, User user, String command) throws BotException {
        String addCityCommand = getLocalizedCommand(command, ADD_CITY_COMMAND);

        log.debug("Request to add new city");
        if (command.toLowerCase().equals(addCityCommand)) {
            return addCityByCallback(message,  chat, user, true);
        }

        String params = command.substring(addCityCommand.length() + 1);

        int i = params.indexOf(" ");
        if (i < 0) {
            commandWaitingService.remove(commandWaitingService.get(chat, user));
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        City newCity = new City();
        newCity.setNameRu(params.substring(0, i));
        newCity.setNameEn(params.substring(i + 1));
        newCity.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()).getID());
        newCity.setUser(user);
        cityService.save(newCity);

        CommandWaiting commandWaiting = commandWaitingService.get(chat, user);
        if (commandWaiting != null && commandWaiting.getCommandName().equals("set")) {
            commandWaitingService.remove(commandWaiting);
        }

        return getKeyboardWithTimeZones(message, newCity.getId());
    }

    private SendMessage deleteCity(Message message, User user, String command) throws BotException {
        String deleteCityCommand = getLocalizedCommand(command, DELETE_CITY_COMMAND);

        log.debug("Request to delete city");

        String params;
        try {
            params = command.substring(deleteCityCommand.length() + 1);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        City city = cityService.get(params);
        if (!city.getUser().getUserId().equals(user.getUserId())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER));
        }

        cityService.remove(city);

        return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.SAVED));
    }

    private EditMessageText setTimeZoneForCity(Message message, Chat chat, User user, String command) throws BotException {
        String setTimezoneCommand = getLocalizedCommand(command, SET_TIMEZONE);

        log.debug("Request to set timezone for city");

        String params;
        try {
            params = command.substring(setTimezoneCommand.length() + 1);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        long cityId;
        TimeZone timeZone;

        int i = params.indexOf(" ");
        if (i < 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        try {
            cityId = Long.parseLong(params.substring(0, i));
            timeZone = TimeZone.getTimeZone(params.substring(i + 1));
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        City city = cityService.get(cityId);
        if (!city.getUser().getUserId().equals(user.getUserId())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER));
        }

        city.setTimeZone(timeZone.getID());
        cityService.save(city);

        return (EditMessageText) getMainKeyboard(message, chat, user, false);
    }

    private PartialBotApiMethod<?> addCityByCallback(Message message,  Chat chat, User user, boolean newMessage) {
        commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_CITY_COMMAND);

        if (newMessage) {
            return buildSendMessageWithText(message, "\n" + ADDING_HELP_TEXT_NAMES);
        }

        return buildEditMessageWithText(message, "\n" + ADDING_HELP_TEXT_NAMES);
    }

    private EditMessageText deleteCityByCallback(Message message, User user, String command) throws BotException {
        String deleteCityCommand = getLocalizedCommand(command, DELETE_CITY_COMMAND);

        log.debug("Request to delete city");

        if (command.toLowerCase().equals(deleteCityCommand)) {
            return getKeyboardWithCities(message, user, CALLBACK_DELETE_CITY_COMMAND);
        }

        long cityId;
        try {
            cityId = Long.parseLong(command.substring(deleteCityCommand.length() + 1));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        City city = cityService.get(cityId);
        if (city == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        List<UserCity> userCities = userCityService.getAll(city);
        if (!userCities.isEmpty()) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.DATA_BASE_INTEGRITY));
        }

        try {
            cityService.remove(city);
        } catch (Exception ignored) {}

        return getKeyboardWithCities(message, user, CALLBACK_DELETE_CITY_COMMAND);
    }

    private EditMessageText getKeyboardWithCities(Message message, User user, String callbackCommand) {
        String emoji;
        String title;
        List<City> cities;

        if (callbackCommand.equals(CALLBACK_DELETE_CITY_COMMAND)) {
            title = "${setter.city.owncitiescaption}";
            emoji = Emoji.DELETE.getEmoji();
            cities = cityService.getAll(user);
        } else {
            title = "${setter.city.selectoradd}";
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

    private PartialBotApiMethod<?> getMainKeyboard(Message message, Chat chat, User user, boolean newMessage) {
        String responseText;

        UserCity userCity = userCityService.get(user, chat);
        if (userCity == null || userCity.getCity() == null) {
            responseText = "${setter.city.citynotset}. ${setter.city.pushbutton} \"${setter.city.button.select}\"";
        } else {
            responseText = "${setter.city.selectedcity}: " + userCity.getCity().getNameRu() + " (" + userCity.getCity().getTimeZone() + ")";
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
        selectButton.setText(Emoji.RIGHT_ARROW_CURVING_UP.getEmoji() + "${setter.city.button.select}");
        selectButton.setCallbackData(CALLBACK_SELECT_CITY_COMMAND);
        selectButtonRow.add(selectButton);

        List<InlineKeyboardButton> addButtonRow = new ArrayList<>();
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText(Emoji.NEW.getEmoji() + "${setter.city.button.add}");
        addButton.setCallbackData(CALLBACK_ADD_CITY_COMMAND);
        addButtonRow.add(addButton);

        List<InlineKeyboardButton> deleteButtonRow = new ArrayList<>();
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText(Emoji.DELETE.getEmoji() + "${setter.city.button.remove}");
        deleteButton.setCallbackData(CALLBACK_DELETE_CITY_COMMAND);
        deleteButtonRow.add(deleteButton);

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getEmoji() + "${setter.city.button.update}");
        updateButton.setCallbackData(CALLBACK_COMMAND + UPDATE_CITY_COMMAND);
        updateButtonRow.add(updateButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getEmoji() + "${setter.city.button.settings}");
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

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setText("\n" + ADDING_HELP_TEXT_TIMEZONE);
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
