package org.telegram.bot.services.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@RequiredArgsConstructor
@Service
@Slf4j
public class SendDocumentExecutor implements MethodExecutor {

    private final TelegramClient telegramClient;
    private final BotStats botStats;
    private final LanguageResolver languageResolver;
    private final InternationalizationService internationalizationService;

    @Override
    public String getMethod() {
        return SendDocument.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, BotRequest request) {
        Message message = request.getMessage();
        String lang = languageResolver.getChatLanguageCode(request);
        SendDocument sendDocument = internationalizationService.internationalize((SendDocument) method, lang);
        log.info("To {}: sending document {}", message.getChatId(), sendDocument.getCaption());

        try {
            telegramClient.execute(sendDocument);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(request, method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
        } catch (Exception e) {
            botStats.incrementErrors(request, method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method) {
        SendDocument sendDocument = (SendDocument) method;

        String chatId = sendDocument.getChatId();
        String lang = languageResolver.getChatLanguageCode(chatId);

        sendDocument = internationalizationService.internationalize(sendDocument, lang);

        log.info("To {}: sending document {}", chatId, sendDocument.getCaption());

        try {
            telegramClient.execute(sendDocument);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
        } catch (Exception e) {
            botStats.incrementErrors(method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }
}
