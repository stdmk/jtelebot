package org.telegram.bot.services.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RequiredArgsConstructor
@Service
@Slf4j
public class SendDocumentExecutor implements MethodExecutor {

    private final Bot bot;
    private final BotStats botStats;

    @Override
    public String getMethod() {
        return SendDocument.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, Message message) {
        SendDocument sendDocument = (SendDocument) method;
        log.info("To " + message.getChatId() + ": sending document " + sendDocument.getCaption());

        try {
            bot.execute(sendDocument);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(message, method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
        } catch (Exception e) {
            botStats.incrementErrors(message, method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }
}
