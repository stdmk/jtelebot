package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatResultsSettings;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatResultsSettingsService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TelegramUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResultsSetter implements Setter<BotResponse> {

    private static final String CALLBACK_COMMAND = "${setter.command} ";
    private static final String EMPTY_RESULTS_COMMAND = "results";
    private static final String SET_RESULTS_COMMAND = "turn";
    private static final String SET_RESULTS_ON_COMMAND = EMPTY_RESULTS_COMMAND + SET_RESULTS_COMMAND + "on";
    private static final String SET_RESULTS_OFF_COMMAND = EMPTY_RESULTS_COMMAND + SET_RESULTS_COMMAND + "off";
    private static final String CALLBACK_SET_RESULTS_ON_COMMAND = CALLBACK_COMMAND + SET_RESULTS_ON_COMMAND;
    private static final String CALLBACK_SET_RESULTS_OFF_COMMAND = CALLBACK_COMMAND + SET_RESULTS_OFF_COMMAND;

    private final ChatResultsSettingsService chatResultsSettingsService;
    private final SpeechService speechService;

    @Override
    public boolean canProcessed(String command) {
        return command.startsWith(EMPTY_RESULTS_COMMAND);
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.MODERATOR;
    }

    @Override
    public BotResponse set(BotRequest request, String commandText) {
        Message message = request.getMessage();
        Chat chat = message.getChat();

        if (TelegramUtils.isPrivateChat(chat)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS));
        }

        String lowerCaseCommandText = commandText.toLowerCase(Locale.ROOT);

        if (message.isCallback() ) {
            if (lowerCaseCommandText.contains(SET_RESULTS_COMMAND)) {
                return setResultsByCallback(message, chat, lowerCaseCommandText);
            } else if (EMPTY_RESULTS_COMMAND.contains(lowerCaseCommandText)) {
                return getResultsSetterWithKeyboard(message, chat, false);
            }
        }

        if (EMPTY_RESULTS_COMMAND.contains(lowerCaseCommandText)) {
            return getResultsSetterWithKeyboard(message, chat, true);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private BotResponse setResultsByCallback(Message message, Chat chat, String command) {
        ChatResultsSettings chatResultsSettings = chatResultsSettingsService.getAllEnabled(chat);
        if (chatResultsSettings == null) {
            chatResultsSettings = new ChatResultsSettings()
                    .setChat(chat);
        }

        chatResultsSettings.setEnabled(command.contains(SET_RESULTS_ON_COMMAND));

        chatResultsSettingsService.save(chatResultsSettings);

        return getResultsSetterWithKeyboard(message, chat, false);
    }

    private BotResponse getResultsSetterWithKeyboard(Message message, Chat chat, boolean newMessage) {
        ChatResultsSettings chatResultsSettings = chatResultsSettingsService.getAllEnabled(chat);

        String enabled;
        if (chatResultsSettings != null && Boolean.TRUE.equals(chatResultsSettings.getEnabled())) {
            enabled = "${setter.results.on}";
        } else {
            enabled = "${setter.results.off}";
        }

        String responseText = "${setter.results.caption}: <b>" + enabled + "</b>";

        if (newMessage) {
            return new TextResponse(message)
                    .setText(responseText)
                    .setKeyboard(prepareKeyboardWithDegreeButtons())
                    .setResponseSettings(FormattingStyle.HTML);
        }

        return new EditResponse(message)
                .setText(responseText)
                .setKeyboard(prepareKeyboardWithDegreeButtons())
                .setResponseSettings(FormattingStyle.HTML);
    }

    private Keyboard prepareKeyboardWithDegreeButtons() {
        return new Keyboard(List.of(
                List.of(new KeyboardButton()
                                .setName(Emoji.CHECK_MARK_BUTTON.getSymbol() + "${setter.results.button.turnon}")
                                .setCallback(CALLBACK_SET_RESULTS_ON_COMMAND),
                        new KeyboardButton()
                                .setName(Emoji.DELETE.getSymbol() + "${setter.results.button.turnoff}")
                                .setCallback(CALLBACK_SET_RESULTS_OFF_COMMAND)),
                List.of(new KeyboardButton()
                        .setName(Emoji.BACK.getSymbol() + "${setter.results.button.settings}")
                        .setCallback(CALLBACK_COMMAND + "back"))));
    }

}
