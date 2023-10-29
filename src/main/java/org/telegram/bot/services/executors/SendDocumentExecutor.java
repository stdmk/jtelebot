package org.telegram.bot.services.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.utils.TelegramUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RequiredArgsConstructor
@Service
@Slf4j
public class SendDocumentExecutor implements MethodExecutor {

    private final Bot bot;
    private final BotStats botStats;
    private final LanguageResolver languageResolver;
    private final InternationalizationService internationalizationService;

    @Override
    public String getMethod() {
        return SendDocument.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, Update update) {
        Message message = TelegramUtils.getMessage(update);
        String lang = languageResolver.getChatLanguageCode(update);
        SendDocument sendDocument = internationalizationService.internationalize((SendDocument) method, lang);
        log.info("To " + message.getChatId() + ": sending document " + sendDocument.getCaption());

        try {
            bot.execute(sendDocument);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(update, method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
        } catch (Exception e) {
            botStats.incrementErrors(update, method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method) {
        SendDocument sendDocument = (SendDocument) method;

        String chatId = sendDocument.getChatId();
        String lang = languageResolver.getChatLanguageCode(chatId);

        sendDocument = internationalizationService.internationalize(sendDocument, lang);

        log.info("To " + chatId + ": sending document " + sendDocument.getCaption());

        try {
            bot.execute(sendDocument);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
        } catch (Exception e) {
            botStats.incrementErrors(method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }
}
