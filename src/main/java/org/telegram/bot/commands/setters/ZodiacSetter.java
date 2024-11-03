package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserZodiac;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.*;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserZodiacService;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.containsStartWith;
import static org.telegram.bot.utils.TextUtils.getStartsWith;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZodiacSetter implements Setter<BotResponse> {

    private static final String CALLBACK_COMMAND = "${setter.command} ";
    private static final String SELECT_ZODIAC_COMMAND = "зодиак ${setter.zodiac.select}";
    private static final String UPDATE_ZODIAC_COMMAND = "зодиак ${setter.zodiac.update}";
    private static final String CALLBACK_SELECT_ZODIAC_COMMAND = CALLBACK_COMMAND + SELECT_ZODIAC_COMMAND;

    private final java.util.Set<String> emptyZodiacCommands = new HashSet<>();
    private final java.util.Set<String> selectZodiacCommands = new HashSet<>();
    private final java.util.Set<String> updateZodiacCommands = new HashSet<>();

    private final UserZodiacService userZodiacService;
    private final SpeechService speechService;
    private final InternationalizationService internationalizationService;

    @PostConstruct
    private void postConstruct() {
        emptyZodiacCommands.addAll(internationalizationService.getAllTranslations("setter.zodiac.emptycommand"));
        selectZodiacCommands.addAll(internationalizationService.internationalize(SELECT_ZODIAC_COMMAND));
        updateZodiacCommands.addAll(internationalizationService.internationalize(UPDATE_ZODIAC_COMMAND));
    }

    @Override
    public boolean canProcessed(String command) {
        return emptyZodiacCommands.stream().anyMatch(command::startsWith);
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
            if (emptyZodiacCommands.contains(lowerCaseCommandText) || updateZodiacCommands.contains(lowerCaseCommandText)) {
                return getUserZodiacWithKeyboard(message, chat, user, false);
            } else if (containsStartWith(selectZodiacCommands, lowerCaseCommandText)) {
                return selectZodiacByCallback(message, chat, user, commandText);
            }
        }

        if (emptyZodiacCommands.contains(lowerCaseCommandText)) {
            return getUserZodiacWithKeyboard(message,  chat, user, true);
        } else if (containsStartWith(selectZodiacCommands, lowerCaseCommandText)) {
            return selectUserZodiac(message, chat, user, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private BotResponse selectZodiacByCallback(Message message, Chat chat, User user, String command) throws BotException {
        String selectZodiacCommand = getLocalizedCommand(command);
        if (command.equals(selectZodiacCommand)) {
            return getUserZodiacWithKeyboard(message, chat, user, false);
        }

        Zodiac zodiac;
        try {
            zodiac = Zodiac.valueOf(command.substring(selectZodiacCommand.length() + 1));
        } catch (IllegalArgumentException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        UserZodiac userZodiac = userZodiacService.get(chat, user);
        if (userZodiac == null) {
            userZodiac = new UserZodiac().setChat(chat).setUser(user);
        }
        userZodiac.setZodiac(zodiac);

        userZodiacService.save(userZodiac);

        return getUserZodiacWithKeyboard(message, chat, user, false);
    }

    private BotResponse selectUserZodiac(Message message, Chat chat, User user, String command) throws BotException {
        String selectZodiacCommand = getLocalizedCommand(command);
        log.debug("Request to select userTv");

        if (command.equals(selectZodiacCommand)) {
            return selectZodiacByCallback(message,  chat, user, SELECT_ZODIAC_COMMAND);
        }

        Zodiac zodiac;
        try {
            zodiac = Zodiac.findByNames((command.substring(selectZodiacCommand.length() + 1)).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        UserZodiac userZodiac = userZodiacService.get(chat, user);
        if (userZodiac == null) {
            userZodiac = new UserZodiac().setChat(chat).setUser(user);
        }
        userZodiac.setZodiac(zodiac);

        userZodiacService.save(userZodiac);

        return new TextResponse(message)
                .setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED))
                .setResponseSettings(FormattingStyle.MARKDOWN);
    }

    private BotResponse getUserZodiacWithKeyboard(Message message, Chat chat, User user, boolean newMessage) {
        UserZodiac userZodiac = userZodiacService.get(chat, user);

        String zodiacName;
        if (userZodiac == null) {
            zodiacName = "${setter.zodiac.notset}";
        } else {
            zodiacName = userZodiac.getZodiac().getName();
        }

        if (newMessage) {
            return new TextResponse(message)
                    .setText("${setter.zodiac.chosenzodiac}: <b>" + zodiacName + "</b>")
                    .setKeyboard(prepareKeyboardWithUserTvList())
                    .setResponseSettings(FormattingStyle.HTML);
        }

        return new EditResponse(message)
                .setText("${setter.zodiac.chosenzodiac}: <b>" + zodiacName + "</b>")
                .setKeyboard(prepareKeyboardWithUserTvList())
                .setResponseSettings(FormattingStyle.HTML);
    }

    private Keyboard prepareKeyboardWithUserTvList() {
        List<List<KeyboardButton>> rows = Arrays.stream(Zodiac.values()).map(zodiac -> List.of(
                new KeyboardButton()
                        .setName(zodiac.getEmoji() + zodiac.getName())
                        .setCallback(CALLBACK_SELECT_ZODIAC_COMMAND + " " + zodiac.name()))).collect(Collectors.toList());

        rows.add(List.of(new KeyboardButton()
                .setName(Emoji.UPDATE.getSymbol() + "${setter.zodiac.button.update}")
                .setCallback(CALLBACK_COMMAND + UPDATE_ZODIAC_COMMAND)));
        rows.add(List.of(new KeyboardButton()
                .setName(Emoji.BACK.getSymbol() + "${setter.zodiac.button.settings}")
                .setCallback(CALLBACK_COMMAND + "back")));

        return new Keyboard(rows);
    }

    private String getLocalizedCommand(String text) {
        String localizedCommand = getStartsWith(
                internationalizationService.internationalize(ZodiacSetter.SELECT_ZODIAC_COMMAND),
                text.toLowerCase(Locale.ROOT));

        if (localizedCommand == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        return localizedCommand;
    }

}
