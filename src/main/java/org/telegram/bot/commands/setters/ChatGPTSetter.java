package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatGPTMessage;
import org.telegram.bot.domain.entities.ChatGPTSettings;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.services.ChatGPTMessageService;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.services.ChatGPTSettingService;
import org.telegram.bot.services.InternationalizationService;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatGPTSetter implements Setter<BotResponse> {

    private static final String CALLBACK_COMMAND = "${setter.command} ";
    private static final String EMPTY_CHATGPT_COMMAND = "chatgpt";
    private static final String RESET_CACHE_COMMAND = EMPTY_CHATGPT_COMMAND + "rc";
    private static final String CALLBACK_RESET_CACHE_COMMAND = CALLBACK_COMMAND + RESET_CACHE_COMMAND;
    private static final String SELECT_MODEL_COMMAND = EMPTY_CHATGPT_COMMAND + "md";
    private static final String CALLBACK_SELECT_MODEL_COMMAND = CALLBACK_COMMAND + SELECT_MODEL_COMMAND;

    private final Set<String> emptyGptCommands = new HashSet<>();

    private final ChatGPTMessageService chatGPTMessageService;
    private final ChatGPTSettingService chatGPTSettingService;
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
    public BotResponse set(BotRequest request, String commandText) {
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();
        String lowerCaseCommandText = commandText.toLowerCase();

        if (message.isCallback()) {
            if (lowerCaseCommandText.equals(RESET_CACHE_COMMAND)) {
                return resetCacheByCallback(message, chat, user);
            } else if (lowerCaseCommandText.startsWith(SELECT_MODEL_COMMAND)) {
                return selectModelByCallback(message, chat, user, commandText);
            }

            return getSetterWithKeyboard(message, chat, user, false);
        }

        if (lowerCaseCommandText.equals(RESET_CACHE_COMMAND)) {
            return resetCacheByCallback(message, chat, user);
        }
        return getSetterWithKeyboard(message, chat, user, true);
    }

    private BotResponse resetCacheByCallback(Message message, Chat chat, User user) {
        if (chat.getChatId() < 0) {
            chatGPTMessageService.reset(chat);
        } else {
            chatGPTMessageService.reset(user);
        }

        return getSetterWithKeyboard(message, chat, user, false);
    }

    private BotResponse selectModelByCallback(Message message, Chat chat, User user, String command) {
        ChatGPTSettings chatGPTSettings = chatGPTSettingService.get(chat);
        if (chatGPTSettings == null) {
            chatGPTSettings = new ChatGPTSettings().setChat(chat);
        }

        chatGPTSettingService.save(chatGPTSettings.setModel(command.substring(SELECT_MODEL_COMMAND.length())));

        return getSetterWithKeyboard(message, chat, user, false);
    }

    private BotResponse getSetterWithKeyboard(Message message, Chat chat, User user, boolean newMessage) {
        List<ChatGPTMessage> messages;
        if (chat.getChatId() < 0) {
            messages = chatGPTMessageService.getMessages(chat);
        } else {
            messages = chatGPTMessageService.getMessages(user);
        }

        String responseText = "${setter.chatgpt.currentcontext}: <b>" + messages.size() + " ${setter.chatgpt.messages}</b>\n" +
                "Max: <b>" + propertiesConfig.getChatGPTContextSize() + "</b>";

        if (newMessage) {
            return new TextResponse(message)
                    .setText(responseText)
                    .setKeyboard(prepareKeyboardWithResetCacheButton(chat))
                    .setResponseSettings(FormattingStyle.HTML);
        }

        return new EditResponse(message)
                .setText(responseText)
                .setKeyboard(prepareKeyboardWithResetCacheButton(chat))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private Keyboard prepareKeyboardWithResetCacheButton(Chat chat) {
        List<KeyboardButton> availableModels = propertiesConfig.getChatGPTModelsAvailable()
                .stream()
                .map(modelName -> new KeyboardButton().setName(modelName).setCallback(CALLBACK_SELECT_MODEL_COMMAND + modelName))
                .collect(Collectors.toList());

        ChatGPTSettings chatGPTSettings = chatGPTSettingService.get(chat);
        if (chatGPTSettings != null) {
            String currentModel = chatGPTSettings.getModel();
            if (currentModel != null) {
                availableModels
                        .stream()
                        .filter(keyboardButton -> currentModel.equals(keyboardButton.getName()))
                        .findFirst()
                        .ifPresent(currentModelKeyboardButton ->
                                currentModelKeyboardButton.setName(Emoji.CHECK_MARK.getSymbol() + currentModelKeyboardButton.getName()));
            }
        }

        List<List<KeyboardButton>> buttonsRows = availableModels.stream().map(List::of).collect(Collectors.toList());
        buttonsRows.add(List.of(new KeyboardButton()
                .setName(Emoji.WASTEBASKET.getSymbol() + "${setter.chatgpt.button.resetcache}")
                .setCallback(CALLBACK_RESET_CACHE_COMMAND)));
        buttonsRows.add(List.of(new KeyboardButton()
                .setName(Emoji.BACK.getSymbol() + "${setter.chatgpt.button.settings}")
                .setCallback(CALLBACK_COMMAND + "back")));

        return new Keyboard(buttonsRows);
    }
}
