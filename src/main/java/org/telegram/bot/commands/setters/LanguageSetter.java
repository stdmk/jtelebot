package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatLanguage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserLanguage;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LanguageSetter implements Setter<BotResponse> {

    private static final String CALLBACK_COMMAND = "${setter.command} ";
    private static final String EMPTY_LANG_COMMAND = "${setter.language.emptycommand}";
    private static final String CHAT = "${setter.language.setchat} ";
    private static final String USER = "${setter.language.setuser} ";
    private static final String CALLBACK_SET_LANG_COMMAND = CALLBACK_COMMAND + EMPTY_LANG_COMMAND;

    private final Set<String> emptyLangCommands = new HashSet<>();
    private final Set<String> chatNames = new HashSet<>();
    private final Set<String> userNames = new HashSet<>();

    private final UserService userService;
    private final ChatLanguageService chatLanguageService;
    private final UserLanguageService userLanguageService;
    private final InternationalizationService internationalizationService;
    private final SpeechService speechService;

    @PostConstruct
    private void postConstruct() {
        emptyLangCommands.addAll(internationalizationService.getAllTranslations("setter.language.emptycommand"));
        chatNames.addAll(internationalizationService.getAllTranslations("setter.language.setchat"));
        userNames.addAll(internationalizationService.getAllTranslations("setter.language.setuser"));
    }

    @Override
    public boolean canProcessed(String command) {
        return emptyLangCommands.stream().anyMatch(command::startsWith);
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FAMILIAR;
    }

    @Override
    public BotResponse set(BotRequest request, String commandText) {
        Message message = request.getMessage();
        Long chatId = message.getChatId();
        Chat chat = message.getChat();
        User user = message.getUser();
        Long userId = user.getUserId();

        String lowerCaseCommandText = commandText.toLowerCase();

        if (message.isCallback()) {
            if (chatId < 0) checkAccessLevelForGroupChat(userId);

            if (emptyLangCommands.contains(lowerCaseCommandText)) {
                return getLangSetterWithKeyboard(message, chat, user, false);
            } else {
                return selectLangByCallback(message, chat, user, lowerCaseCommandText);
            }
        }

        if (chatId < 0) checkAccessLevelForGroupChat(userId);

        if (emptyLangCommands.contains(lowerCaseCommandText)) {
            return getLangSetterWithKeyboard(message, chat, user, true);
        } else {
            return setLang(message, chat, user, lowerCaseCommandText);
        }
    }

    private void checkAccessLevelForGroupChat(Long userId) {
        Integer userAccessLevel = userService.getUserAccessLevel(userId);
        if (!userService.isUserHaveAccessForCommand(userAccessLevel, AccessLevel.MODERATOR.getValue())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_ACCESS));
        }
    }

    private BotResponse selectLangByCallback(Message message, Chat chat, User user, String command) {
        if (emptyLangCommands.contains(command)) {
            return getLangSetterWithKeyboard(message, chat, user, false);
        }

        String emptyCommand = emptyLangCommands
                .stream()
                .filter(command::startsWith)
                .findFirst()
                .orElseThrow(() -> new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)));

        String params = command.substring(emptyCommand.length());

        int spaceIndex = params.indexOf(" ");
        if (spaceIndex <= 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
        String entity = params.substring(0, params.indexOf(" "));
        String lang = params.substring(params.indexOf(" ") + 1);

        if (!internationalizationService.getAvailableLocales().contains(lang)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        if (userNames.contains(entity)) {
            userLanguageService.save(chat, user, lang);
        } else {
            if (chatNames.contains(entity)) {
                if (chat.getChatId() > 0) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                }
                chatLanguageService.save(chat, lang);
            } else {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }
        }

        return getLangSetterWithKeyboard(message, chat, user, false);
    }

    private BotResponse setLang(Message message, Chat chat, User user, String command) {
        if (emptyLangCommands.contains(command)) {
            return getLangSetterWithKeyboard(message, chat, user, true);
        }

        String emptyCommand = emptyLangCommands
                .stream()
                .filter(command::startsWith)
                .findFirst()
                .orElseThrow(() -> new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)));

        String params = command.substring(emptyCommand.length() + 1);

        int spaceIndex = params.indexOf(" ");
        if (spaceIndex <= 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
        String entity = params.substring(0, spaceIndex);
        String lang = params.substring(spaceIndex + 1);

        if (userNames.contains(entity)) {
            userLanguageService.save(chat, user, lang);
        } else {
            if (chatNames.contains(entity)) {
                if (chat.getChatId() > 0) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS));
                }
                chatLanguageService.save(chat, lang);
            } else {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }

        return new TextResponse(message)
                .setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED));
    }

    private BotResponse getLangSetterWithKeyboard(Message message, Chat chat, User user, boolean newMessage) {
        log.debug("Request to get language setter for chat {}", chat.getChatId());

        ChatLanguage chatLanguage = chatLanguageService.get(chat);
        String setChatLang;
        if (chatLanguage == null) {
            setChatLang = "${setter.language.notsetted}";
        } else {
            setChatLang = chatLanguage.getLang();
        }

        UserLanguage userLanguage = userLanguageService.get(chat, user);
        String setUserLang;
        if (userLanguage == null) {
            setUserLang = "${setter.language.notsetted}";
        } else {
            setUserLang = userLanguage.getLang();
        }

        String responseText = "${setter.language.currentuser}: <b>" + setUserLang + "</b>.\n"
                + "${setter.language.currentchat}: <b>" + setChatLang + "</b>";

        if (newMessage) {
            return new TextResponse(message)
                    .setText(responseText)
                    .setKeyboard(prepareKeyboardWithLangButtons(message))
                    .setResponseSettings(FormattingStyle.HTML);
        }

        return new EditResponse(message)
                .setText(responseText)
                .setKeyboard(prepareKeyboardWithLangButtons(message))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private Keyboard prepareKeyboardWithLangButtons(Message message) {
        Set<String> availableLocales = internationalizationService.getAvailableLocales();
        List<List<KeyboardButton>> buttonsList = new ArrayList<>(3);

        buttonsList.add(availableLocales.stream().map(locale -> new KeyboardButton()
                .setName(USER + locale)
                .setCallback(CALLBACK_SET_LANG_COMMAND + USER + locale)
        ).collect(Collectors.toList()));

        if (message.isGroupChat()) {
            buttonsList.add(availableLocales.stream().map(locale -> new KeyboardButton()
                    .setName(CHAT + locale)
                    .setCallback(CALLBACK_SET_LANG_COMMAND + CHAT + locale)
            ).collect(Collectors.toList()));
        }

        buttonsList.add(List.of(
                new KeyboardButton()
                        .setName(Emoji.BACK.getSymbol() + "${setter.language.button.settings}")
                        .setCallback(CALLBACK_COMMAND + "back")));

        return new Keyboard(buttonsList);
    }

}
