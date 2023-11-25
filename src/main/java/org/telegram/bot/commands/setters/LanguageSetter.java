package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatLanguage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserLanguage;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LanguageSetter implements Setter<PartialBotApiMethod<?>> {

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
    public PartialBotApiMethod<?> set(Update update, String commandText) {
        Message message = getMessageFromUpdate(update);
        Long chatId = message.getChatId();
        Chat chat = new Chat().setChatId(chatId);

        String lowerCaseCommandText = commandText.toLowerCase();

        if (update.hasCallbackQuery()) {
            Long userId = update.getCallbackQuery().getFrom().getId();
            if (chatId < 0) checkAccessLevelForGroupChat(userId);

            User user = new User().setUserId(userId);
            if (emptyLangCommands.contains(lowerCaseCommandText)) {
                return getLangSetterWithKeyboard(message, chat, user, false);
            } else {
                return selectLangByCallback(message, chat, user, lowerCaseCommandText);
            }
        }

        Long userId = message.getFrom().getId();
        if (chatId < 0) checkAccessLevelForGroupChat(userId);

        User user = new User().setUserId(userId);
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

    private PartialBotApiMethod<?> selectLangByCallback(Message message, Chat chat, User user, String command) {
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

    private PartialBotApiMethod<?> setLang(Message message, Chat chat, User user, String command) {
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

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED));

        return sendMessage;
    }

    private PartialBotApiMethod<?> getLangSetterWithKeyboard(Message message, Chat chat, User user, Boolean newMessage) {
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
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setText(responseText);
            sendMessage.setReplyMarkup(prepareKeyboardWithLangButtons());

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(responseText);
        editMessageText.setReplyMarkup(prepareKeyboardWithLangButtons());

        return editMessageText;
    }

    private InlineKeyboardMarkup prepareKeyboardWithLangButtons() {
        Set<String> availableLocales = internationalizationService.getAvailableLocales();

        List<InlineKeyboardButton> setUserLangRow = new ArrayList<>();
        availableLocales.forEach(locale -> {
            InlineKeyboardButton setLangButton = new InlineKeyboardButton();
            setLangButton.setText(USER + locale);
            setLangButton.setCallbackData(CALLBACK_SET_LANG_COMMAND + USER + locale);
            setUserLangRow.add(setLangButton);
        });

        List<InlineKeyboardButton> setChatLangRow = new ArrayList<>();
        availableLocales.forEach(locale -> {
            InlineKeyboardButton setLangButton = new InlineKeyboardButton();
            setLangButton.setText(CHAT + locale);
            setLangButton.setCallbackData(CALLBACK_SET_LANG_COMMAND + CHAT + locale);
            setChatLangRow.add(setLangButton);
        });

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getEmoji() + "${setter.language.button.settings}");
        backButton.setCallbackData(CALLBACK_COMMAND + "back");
        backButtonRow.add(backButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(setUserLangRow);
        rows.add(setChatLangRow);
        rows.add(backButtonRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

}
