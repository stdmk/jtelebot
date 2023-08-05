package org.telegram.bot.services.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RequiredArgsConstructor
@Service
@Slf4j
public class SendPhotoExecutor implements MethodExecutor {

    private final Bot bot;
    private final BotStats botStats;

    @Override
    public String getMethod() {
        return SendPhoto.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, Message message) {
        SendPhoto sendPhoto = (SendPhoto) method;
        log.info("To " + message.getChatId() + ": sending photo " + sendPhoto.getCaption());

        try {
            bot.execute(sendPhoto);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(message, method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
            tryToDeliverTheMessage(sendPhoto);
        } catch (Exception e) {
            botStats.incrementErrors(message, method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }

    private void tryToDeliverTheMessage(SendPhoto sendPhoto) {
        String imageUrl = sendPhoto.getPhoto().getAttachName();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyToMessageId(sendPhoto.getReplyToMessageId());
        sendMessage.setChatId(sendPhoto.getChatId());
        sendMessage.setText("Не удалось отправить картинку с адреса: " + imageUrl + "\n" + sendPhoto.getCaption());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();

        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Still failed to send response: {}", e.getMessage());
        }
    }
}
