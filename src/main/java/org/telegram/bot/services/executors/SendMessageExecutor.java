package org.telegram.bot.services.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RequiredArgsConstructor
@Service
@Slf4j
public class SendMessageExecutor implements MethodExecutor {

    private final Bot bot;
    private final BotStats botStats;

    @Override
    public String getMethod() {
        return SendMessage.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, Message message) {
        SendMessage sendMessage = (SendMessage) method;
        log.info("To " + message.getChatId() + ": " + sendMessage.getText());

        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(message, method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
            tryToSendWithoutMarkdowns(sendMessage);
        } catch (Exception e) {
            botStats.incrementErrors(message, method, e, "unexpected error");
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
