package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.config.PropertiesConfig;
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
import org.telegram.bot.services.ChatGPTSettingService;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.InternationalizationService;

import javax.annotation.PostConstruct;
import java.util.*;
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
    private static final String SET_MODEL = EMPTY_CHATGPT_COMMAND + "cmd";
    private static final String CALLBACK_SET_MODEL = CALLBACK_COMMAND + EMPTY_CHATGPT_COMMAND + "cmd";
    private static final String SET_PROMPT = EMPTY_CHATGPT_COMMAND + "pr";
    private static final String CALLBACK_SET_PROMPT = CALLBACK_COMMAND + EMPTY_CHATGPT_COMMAND + "pr";

    private final Set<String> emptyGptCommands = new HashSet<>();

    private final ChatGPTMessageService chatGPTMessageService;
    private final ChatGPTSettingService chatGPTSettingService;
    private final InternationalizationService internationalizationService;
    private final PropertiesConfig propertiesConfig;
    private final CommandWaitingService commandWaitingService;

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
        commandWaitingService.remove(chat, user);

        String lowerCaseCommandText = commandText.toLowerCase(Locale.ROOT);
        if (message.isCallback()) {
            if (lowerCaseCommandText.equals(RESET_CACHE_COMMAND)) {
                return resetCacheByCallback(message, chat, user);
            } else if (lowerCaseCommandText.startsWith(SELECT_MODEL_COMMAND)) {
                return selectModelByCallback(message, chat, user, commandText);
            } else if (lowerCaseCommandText.startsWith(SET_MODEL)) {
                return setModelByCallback(message, chat, user);
            } else if (lowerCaseCommandText.startsWith(SET_PROMPT)) {
                return setPromptByCallback(message, chat, user);
            }

            return getSetterWithKeyboard(message, chat, user, false);
        }

        if (lowerCaseCommandText.equals(RESET_CACHE_COMMAND)) {
            return resetCacheByCallback(message, chat, user);
        } else if (lowerCaseCommandText.startsWith(SET_MODEL)) {
            return setModel(message, chat, user, commandText);
        } else if (lowerCaseCommandText.startsWith(SET_PROMPT)) {
            return setPrompt(message, chat, user, commandText);
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
        commandWaitingService.add(chat, user, org.telegram.bot.commands.Set.class, CALLBACK_SELECT_MODEL_COMMAND);

        ChatGPTSettings chatGPTSettings = chatGPTSettingService.get(chat);
        if (chatGPTSettings == null) {
            chatGPTSettings = new ChatGPTSettings().setChat(chat);
        }

        chatGPTSettingService.save(chatGPTSettings.setModel(command.substring(SELECT_MODEL_COMMAND.length())));

        return getSetterWithKeyboard(message, chat, user, false);
    }

    private EditResponse setModelByCallback(Message message, Chat chat, User user) {
        commandWaitingService.add(chat, user, org.telegram.bot.commands.Set.class, CALLBACK_SET_MODEL);

        return new EditResponse(message)
                .setText("${setter.chatgpt.setmodelhelp}")
                .setResponseSettings(FormattingStyle.HTML);
    }

    private BotResponse setModel(Message message, Chat chat, User user, String command) {
        ChatGPTSettings chatGPTSettings = chatGPTSettingService.get(chat);
        if (chatGPTSettings == null) {
            chatGPTSettings = new ChatGPTSettings().setChat(chat);
        }

        chatGPTSettingService.save(chatGPTSettings.setModel(command.substring(SET_MODEL.length())));

        return getSetterWithKeyboard(message, chat, user, true);
    }

    private EditResponse setPromptByCallback(Message message, Chat chat, User user) {
        commandWaitingService.add(chat, user, org.telegram.bot.commands.Set.class, CALLBACK_SET_PROMPT);

        return new EditResponse(message)
                .setText("${setter.chatgpt.setprompthelp}")
                .setResponseSettings(FormattingStyle.HTML);
    }

    private BotResponse setPrompt(Message message, Chat chat, User user, String command) {
        ChatGPTSettings chatGPTSettings = chatGPTSettingService.get(chat);

        String prompt = command.substring(SET_PROMPT.length() + 1);
        if (chatGPTSettings == null) {
            chatGPTSettings = new ChatGPTSettings().setChat(chat);
        }

        chatGPTSettingService.save(chatGPTSettings.setPrompt(prompt));

        return getSetterWithKeyboard(message, chat, user, true);
    }

    private BotResponse getSetterWithKeyboard(Message message, Chat chat, User user, boolean newMessage) {
        List<ChatGPTMessage> messages;
        if (chat.getChatId() < 0) {
            messages = chatGPTMessageService.getMessages(chat);
        } else {
            messages = chatGPTMessageService.getMessages(user);
        }

        ChatGPTSettings chatGPTSettings = chatGPTSettingService.get(chat);

        String model = "";
        String prompt = "";
        if (chatGPTSettings != null) {
            if (chatGPTSettings.getModel() != null) {
                model = "${setter.chatgpt.currentmodel} <b>" + chatGPTSettings.getModel() + "</b>\n";
            }
            if (chatGPTSettings.getPrompt() != null) {
                prompt = "${setter.chatgpt.currentprompt}: " + chatGPTSettings.getPrompt();
            }
        }

        String responseText = model
                + "${setter.chatgpt.currentcontext}: <b>" + messages.size() + " ${setter.chatgpt.messages}</b>\n"
                + "Max: <b>" + propertiesConfig.getChatGPTContextSize() + "</b>\n"
                + prompt + "\n";

        if (newMessage) {
            return new TextResponse(message)
                    .setText(responseText)
                    .setKeyboard(prepareKeyboard(chatGPTSettings))
                    .setResponseSettings(FormattingStyle.HTML);
        }

        return new EditResponse(message)
                .setText(responseText)
                .setKeyboard(prepareKeyboard(chatGPTSettings))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private Keyboard prepareKeyboard(ChatGPTSettings chatGPTSettings) {
        List<String> availableModels = propertiesConfig.getChatGPTModelsAvailable();
        List<KeyboardButton> selectModelButtons;
        if (availableModels == null) {
            selectModelButtons = new ArrayList<>(2);
        } else {
            selectModelButtons = availableModels
                    .stream()
                    .map(modelName -> new KeyboardButton().setName(modelName).setCallback(CALLBACK_SELECT_MODEL_COMMAND + modelName))
                    .toList();
        }

        if (chatGPTSettings != null) {
            String currentModel = chatGPTSettings.getModel();
            if (currentModel != null) {
                selectModelButtons
                        .stream()
                        .filter(keyboardButton -> currentModel.equals(keyboardButton.getName()))
                        .findFirst()
                        .ifPresent(currentModelKeyboardButton ->
                                currentModelKeyboardButton.setName(Emoji.CHECK_MARK.getSymbol() + currentModelKeyboardButton.getName()));
            }
        }

        List<List<KeyboardButton>> buttonsRows = selectModelButtons.stream().map(List::of).collect(Collectors.toList());
        buttonsRows.add(List.of(new KeyboardButton()
                .setName(Emoji.ROBOT.getSymbol() + "${setter.chatgpt.button.setmodel}")
                .setCallback(CALLBACK_SET_MODEL)));
        buttonsRows.add(List.of(new KeyboardButton()
                .setName(Emoji.GEAR.getSymbol() + "${setter.chatgpt.button.setprompt}")
                .setCallback(CALLBACK_SET_PROMPT)));
        buttonsRows.add(List.of(new KeyboardButton()
                .setName(Emoji.WASTEBASKET.getSymbol() + "${setter.chatgpt.button.resetcache}")
                .setCallback(CALLBACK_RESET_CACHE_COMMAND)));
        buttonsRows.add(List.of(new KeyboardButton()
                .setName(Emoji.BACK.getSymbol() + "${setter.chatgpt.button.settings}")
                .setCallback(CALLBACK_COMMAND + "back")));

        return new Keyboard(buttonsRows);
    }
}
