package org.telegram.bot.services.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.utils.ObjectCopier;
import org.telegram.bot.utils.TelegramUtils;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RequiredArgsConstructor
@Service
@Slf4j
public class SendMessageExecutor implements MethodExecutor {

    private final Bot bot;
    private final BotStats botStats;
    private final LanguageResolver languageResolver;
    private final InternationalizationService internationalizationService;
    private final ObjectCopier objectCopier;

    @Override
    public String getMethod() {
        return SendMessage.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, Update update) {
        Message message = TelegramUtils.getMessage(update);
        String lang = languageResolver.getChatLanguageCode(update);
        SendMessage sendMessage = internationalizationService.internationalize((SendMessage) method, lang);

        String messageText = sendMessage.getText();
        log.info("To " + message.getChatId() + ": " + messageText);

        if (TextUtils.isNotTextLengthIncludedInLimit(messageText)) {
            TextUtils.splitTextByTelegramMaxLength(messageText)
                    .stream()
                    .map(text -> {
                        SendMessage splittedSendMessage = objectCopier.copyObject(sendMessage, SendMessage.class);
                        splittedSendMessage.setText(text);
                        return splittedSendMessage;
                    })
                    .forEach(splittedSendMessage -> sendMessage(splittedSendMessage, method, update));
        } else {
            sendMessage(sendMessage, method, update);
        }
    }

    private void sendMessage(SendMessage sendMessage, PartialBotApiMethod<?> method, Update update) {
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(update, method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
            tryToSendWithoutMarkdowns(sendMessage);
        } catch (Exception e) {
            botStats.incrementErrors(update, method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method) {
        SendMessage sendMessage = (SendMessage) method;

        String chatId = sendMessage.getChatId();
        String lang = languageResolver.getChatLanguageCode(chatId);

        sendMessage = internationalizationService.internationalize(sendMessage, lang);

        log.info("To " + chatId + ": " + sendMessage.getText());

        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(method, e, "error sending message");
            log.error("Error: cannot send response: {}", e.getMessage());
            tryToSendWithoutMarkdowns(sendMessage);
        } catch (Exception e) {
            botStats.incrementErrors(method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }

    private void tryToSendWithoutMarkdowns(SendMessage sendMessage) {
        sendMessage.enableMarkdown(false);
        sendMessage.enableHtml(false);
        sendMessage.enableMarkdownV2(false);

        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Still failed to send response: {}", e.getMessage());
        }
    }
}
