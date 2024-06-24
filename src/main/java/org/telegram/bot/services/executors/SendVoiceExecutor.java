package org.telegram.bot.services.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@RequiredArgsConstructor
@Service
@Slf4j
public class SendVoiceExecutor implements MethodExecutor {

    private final TelegramClient telegramClient;
    private final BotStats botStats;

    @Override
    public String getMethod() {
        return SendVoice.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, BotRequest request) {
        Message message = request.getMessage();
        SendVoice sendVoice = (SendVoice) method;
        log.info("To " + message.getChatId() + ": " + sendVoice.getCaption());

        try {
            telegramClient.execute(sendVoice);
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
        SendVoice sendVoice = (SendVoice) method;
        log.info("To " + sendVoice.getChatId() + ": " + sendVoice.getCaption());

        try {
            telegramClient.execute(sendVoice);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
        } catch (Exception e) {
            botStats.incrementErrors(method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }
}
