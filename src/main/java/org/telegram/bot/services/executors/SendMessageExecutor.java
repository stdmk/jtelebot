package org.telegram.bot.services.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.utils.ObjectCopier;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@RequiredArgsConstructor
@Service
@Slf4j
public class SendMessageExecutor implements MethodExecutor {

    private static final String DEFAULT_ERROR_MESSAGE = "error sending message";

    private final BotStats botStats;
    private final LanguageResolver languageResolver;
    private final InternationalizationService internationalizationService;
    private final ObjectCopier objectCopier;
    private final TelegramClient telegramClient;

    @Override
    public String getMethod() {
        return SendMessage.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, BotRequest request) {
        Message message = request.getMessage();
        String lang = languageResolver.getChatLanguageCode(request);
        SendMessage sendMessage = internationalizationService.internationalize((SendMessage) method, lang);

        String messageText = sendMessage.getText();
        if (!StringUtils.hasLength(messageText)) {
            String errorMessage = "attempt to send empty message";
            log.error(errorMessage);
            botStats.incrementErrors(request, errorMessage);
            return;
        }

        log.info("To {}: {}", message.getChatId(), messageText);

        if (TextUtils.isNotTextLengthIncludedInLimit(messageText)) {
            TextUtils.splitTextByTelegramMaxLength(messageText)
                    .stream()
                    .map(text -> {
                        SendMessage splittedSendMessage = objectCopier.copyObject(sendMessage, SendMessage.class);
                        splittedSendMessage.setText(text);
                        return splittedSendMessage;
                    })
                    .forEach(splittedSendMessage -> sendMessage(splittedSendMessage, method, request));
        } else {
            sendMessage(sendMessage, method, request);
        }
    }

    private void sendMessage(SendMessage sendMessage, PartialBotApiMethod<?> method, BotRequest botRequest) {
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            if (isError(e)) {
                botStats.incrementErrors(botRequest, method, e, "error sending response");
            }
            log.error("Error: cannot send response: {}", e.getMessage());
            tryToSendAgain(sendMessage);
        } catch (Exception e) {
            botStats.incrementErrors(botRequest, method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method) {
        SendMessage sendMessage = (SendMessage) method;

        String chatId = sendMessage.getChatId();
        String lang = languageResolver.getChatLanguageCode(chatId);

        sendMessage = internationalizationService.internationalize(sendMessage, lang);

        String messageText = sendMessage.getText();
        if (!StringUtils.hasLength(messageText)) {
            String errorMessage = "attempt to send empty message";
            log.error(errorMessage);
            botStats.incrementErrors(sendMessage, errorMessage);
            return;
        }
        log.info("To {}: {}", chatId, messageText);

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(method, e, DEFAULT_ERROR_MESSAGE);
            log.error("Error: cannot send response: {}", e.getMessage());
            tryToSendAgain(sendMessage);
        } catch (Exception e) {
            botStats.incrementErrors(method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }

    private void tryToSendAgain(SendMessage sendMessage) {
        if (!tryToSendMessageWithoutReplyTo(sendMessage)) {
            tryToSendMessageWithoutMarkdown(sendMessage);
        }
    }

    private boolean tryToSendMessageWithoutMarkdown(SendMessage sendMessage) {
        sendMessage.setParseMode(null);

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(sendMessage, e, DEFAULT_ERROR_MESSAGE);
            log.error("Failed to send response after clearing markdown: {}", e.getMessage());
            return false;
        }

        return true;
    }

    private boolean tryToSendMessageWithoutReplyTo(SendMessage sendMessage) {
        sendMessage.setReplyToMessageId(null);

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(sendMessage, e, DEFAULT_ERROR_MESSAGE);
            log.error("Failed to send response after clearing replying message: {}", e.getMessage());
            return false;
        }

        return true;
    }
}
