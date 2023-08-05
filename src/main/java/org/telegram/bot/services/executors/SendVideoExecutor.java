package org.telegram.bot.services.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RequiredArgsConstructor
@Service
@Slf4j
public class SendVideoExecutor implements MethodExecutor {

    private final Bot bot;
    private final BotStats botStats;

    @Override
    public String getMethod() {
        return SendVideo.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, Message message) {
        SendVideo sendVideo = (SendVideo) method;
        log.info("To " + message.getChatId() + ": " + sendVideo.getCaption());

        try {
            bot.execute(sendVideo);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(message, method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
        } catch (Exception e) {
            botStats.incrementErrors(message, method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }
}
