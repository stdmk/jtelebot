package org.telegram.bot.services.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RequiredArgsConstructor
@Service
@Slf4j
public class SendLocationExecutor implements MethodExecutor {

    private final Bot bot;
    private final BotStats botStats;

    @Override
    public String getMethod() {
        return SendLocation.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, BotRequest request) {
        Message message = request.getMessage();
        SendLocation sendLocation = (SendLocation) method;
        log.info("To " + message.getChatId() + ": " + sendLocation.getLatitude() + " " + sendLocation.getLongitude());

        try {
            bot.execute(sendLocation);
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
        SendLocation sendLocation = (SendLocation) method;
        log.info("To " + sendLocation.getChatId() + ": " + sendLocation.getLatitude() + " " + sendLocation.getLongitude());

        try {
            bot.execute(sendLocation);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
        } catch (Exception e) {
            botStats.incrementErrors(method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }
}
