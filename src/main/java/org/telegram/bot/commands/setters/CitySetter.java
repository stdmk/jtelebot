package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.commands.Set;
import org.telegram.bot.domain.entities.*;
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
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import static org.telegram.bot.utils.DateUtils.TimeZones;
import static org.telegram.bot.utils.TextUtils.containsStartWith;
import static org.telegram.bot.utils.TextUtils.getStartsWith;

@Service
@RequiredArgsConstructor
@Slf4j
public class CitySetter implements Setter<BotResponse> {

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
    public BotResponse set(BotRequest request, String commandText) {
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();
        String lowerCaseCommandText = commandText.toLowerCase();

        if (message.isCallback()) {
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

    private EditResponse selectCityByCallback(Message message, Chat chat, User user, String command) throws BotException {
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

        return (EditResponse) getMainKeyboard(message, chat, user, false);
    }

    private BotResponse addCity(Message message, Chat chat, User user, String command) throws BotException {
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

    private TextResponse deleteCity(Message message, User user, String command) throws BotException {
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

        return new TextResponse(message)
                .setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private EditResponse setTimeZoneForCity(Message message, Chat chat, User user, String command) throws BotException {
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

        return (EditResponse) getMainKeyboard(message, chat, user, false);
    }

    private BotResponse addCityByCallback(Message message,  Chat chat, User user, boolean newMessage) {
        commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_CITY_COMMAND);

        if (newMessage) {
            return new TextResponse(message)
                    .setText("\n" + ADDING_HELP_TEXT_NAMES)
                    .setResponseSettings(FormattingStyle.HTML);
        }

        return new EditResponse(message)
                .setText("\n" + ADDING_HELP_TEXT_NAMES)
                .setResponseSettings(FormattingStyle.HTML);
    }

    private EditResponse deleteCityByCallback(Message message, User user, String command) throws BotException {
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


        cityService.remove(city);

        return getKeyboardWithCities(message, user, CALLBACK_DELETE_CITY_COMMAND);
    }

    private EditResponse getKeyboardWithCities(Message message, User user, String callbackCommand) {
        String emoji;
        String title;
        List<City> cities;

        if (callbackCommand.equals(CALLBACK_DELETE_CITY_COMMAND)) {
            title = "${setter.city.owncitiescaption}";
            emoji = Emoji.DELETE.getSymbol();
            cities = cityService.getAll(user);
        } else {
            title = "${setter.city.selectoradd}";
            emoji = "";
            cities = cityService.getAll();
        }

        List<List<KeyboardButton>> cityRows = cities.stream().map(city -> List.of(new KeyboardButton()
                .setName(emoji + city.getNameRu())
                .setCallback(callbackCommand + " " + city.getId()))).toList();

        return new EditResponse(message)
                .setText(title)
                .setKeyboard(new Keyboard(cityRows))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private BotResponse getMainKeyboard(Message message, Chat chat, User user, boolean newMessage) {
        String responseText;

        UserCity userCity = userCityService.get(user, chat);
        if (userCity == null || userCity.getCity() == null) {
            responseText = "${setter.city.citynotset}. ${setter.city.pushbutton} \"${setter.city.button.select}\"";
        } else {
            responseText = "${setter.city.selectedcity}: " + userCity.getCity().getNameRu() + " (" + userCity.getCity().getTimeZone() + ")";
        }

        if (newMessage) {
            return new TextResponse(message)
                    .setText(responseText)
                    .setKeyboard(prepareMainKeyboard());
        }

        return new EditResponse(message)
                .setText(responseText)
                .setKeyboard(prepareMainKeyboard())
                .setResponseSettings(FormattingStyle.HTML);
    }

    private Keyboard prepareMainKeyboard() {
        return new Keyboard(
                new KeyboardButton()
                        .setName(Emoji.RIGHT_ARROW_CURVING_UP.getSymbol() + "${setter.city.button.select}")
                        .setCallback(CALLBACK_SELECT_CITY_COMMAND),
                new KeyboardButton()
                        .setName(Emoji.NEW.getSymbol() + "${setter.city.button.add}")
                        .setCallback(CALLBACK_ADD_CITY_COMMAND),
                new KeyboardButton()
                        .setName(Emoji.DELETE.getSymbol() + "${setter.city.button.remove}")
                        .setCallback(CALLBACK_DELETE_CITY_COMMAND),
                new KeyboardButton()
                        .setName(Emoji.UPDATE.getSymbol() + "${setter.city.button.update}")
                        .setCallback(CALLBACK_COMMAND + UPDATE_CITY_COMMAND),
                new KeyboardButton()
                        .setName(Emoji.BACK.getSymbol() + "${setter.city.button.settings}")
                        .setCallback(CALLBACK_COMMAND + "back"));
    }

    private TextResponse getKeyboardWithTimeZones(Message message, Long cityId) {
        List<List<KeyboardButton>> rows = Arrays
                .stream(TimeZones.values())
                .map(zone -> List.of(
                        new KeyboardButton()
                                .setName(zone.getZone())
                                .setCallback(CALLBACK_SET_TIMEZONE + " " + cityId + " " + zone.getZone())))
                .toList();

        return new TextResponse(message)
                .setText("\n" + ADDING_HELP_TEXT_TIMEZONE)
                .setKeyboard(new Keyboard(rows))
                .setResponseSettings(FormattingStyle.HTML);
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
