package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatGPTMessage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.services.ChatGPTMessageService;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.services.InternationalizationService;
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
public class ChatGPTSetter implements Setter<PartialBotApiMethod<?>> {

    private static final String CALLBACK_COMMAND = "${setter.command} ";
    private static final String EMPTY_CHATGPT_COMMAND = "chatgpt";
    private static final String RESET_CACHE_COMMAND = EMPTY_CHATGPT_COMMAND + "rc";
    private static final String CALLBACK_RESET_CACHE_COMMAND = CALLBACK_COMMAND + RESET_CACHE_COMMAND;

    private final Set<String> emptyGptCommands = new HashSet<>();

    private final ChatGPTMessageService chatGPTMessageService;
    private final InternationalizationService internationalizationService;
    private final PropertiesConfig propertiesConfig;

    @PostConstruct
    private void postConstruct() {
        emptyGptCommands.addAll(internationalizationService.getAllTranslations("setter.chatgpt.emptycommand"));
    }

    @Override
    public boolean canProcessed(String command) {
        return emptyGptCommands.stream().anyMatch(command::startsWith);
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

            if (lowerCaseCommandText.equals(RESET_CACHE_COMMAND)) {
                return resetCacheByCallback(message, chat, user);
            }
            return getResetCacheSetterWithKeyboard(message, chat, user, false);
        }

        User user = new User().setUserId(message.getFrom().getId());
        if (lowerCaseCommandText.equals(RESET_CACHE_COMMAND)) {
            return resetCacheByCallback(message, chat, user);
        }
        return getResetCacheSetterWithKeyboard(message, chat, user, true);
    }

    private PartialBotApiMethod<?> resetCacheByCallback(Message message, Chat chat, User user) {
        if (chat.getChatId() < 0) {
            chatGPTMessageService.reset(chat);
        } else {
            chatGPTMessageService.reset(user);
        }

        return getResetCacheSetterWithKeyboard(message, chat, user, false);
    }

    private PartialBotApiMethod<?> getResetCacheSetterWithKeyboard(Message message, Chat chat, User user, boolean newMessage) {
        List<ChatGPTMessage> messages;
        if (chat.getChatId() < 0) {
            messages = chatGPTMessageService.getMessages(chat);
        } else {
            messages = chatGPTMessageService.getMessages(user);
        }

        String responseText = "${setter.chatgpt.currentcontext}: <b>" + messages.size() + " ${setter.chatgpt.messages}</b>\n" +
                "Max: <b>" + propertiesConfig.getChatGPTContextSize() + "</b>";

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setText(responseText);
            sendMessage.setReplyMarkup(prepareKeyboardWithResetCacheButton());

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(responseText);
        editMessageText.setReplyMarkup(prepareKeyboardWithResetCacheButton());

        return editMessageText;
    }

    private InlineKeyboardMarkup prepareKeyboardWithResetCacheButton() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> resetCacheButtonRow = new ArrayList<>();
        InlineKeyboardButton resetCacheButton = new InlineKeyboardButton();
        resetCacheButton.setText(Emoji.WASTEBASKET.getSymbol() + "${setter.chatgpt.button.resetcache}");
        resetCacheButton.setCallbackData(CALLBACK_RESET_CACHE_COMMAND);
        resetCacheButtonRow.add(resetCacheButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getSymbol() + "${setter.chatgpt.button.settings}");
        backButton.setCallbackData(CALLBACK_COMMAND + "back");
        backButtonRow.add(backButton);

        rows.add(resetCacheButtonRow);
        rows.add(backButtonRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }
}
