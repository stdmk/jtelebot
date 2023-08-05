package org.telegram.bot.services.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RequiredArgsConstructor
@Service
@Slf4j
public class SendMediaGroupExecutor implements MethodExecutor {

    private final Bot bot;
    private final BotStats botStats;

    @Override
    public String getMethod() {
        return SendMediaGroup.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, Message message) {
        SendMediaGroup sendMediaGroup = (SendMediaGroup) method;
        log.info("To " + message.getChatId() + ": sending photos " + sendMediaGroup);
        try {
            bot.execute(sendMediaGroup);
        } catch (TelegramApiException e) {
            tryToSendOnePhoto(sendMediaGroup);
        } catch (Exception e) {
            botStats.incrementErrors(message, method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }

    private void tryToSendOnePhoto(SendMediaGroup sendMediaGroup) {
        StringBuilder buf = new StringBuilder("Остальные картинки: \n");
        sendMediaGroup.getMedias().stream().skip(1).forEach(inputMedia -> buf.append(inputMedia.getCaption()).append("\n"));

        InputMedia inputMedia = sendMediaGroup.getMedias().get(0);
        InputFile inputFile = new InputFile();
        inputFile.setMedia(inputMedia.getMedia());

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(inputFile);
        sendPhoto.setReplyToMessageId(sendMediaGroup.getReplyToMessageId());
        sendPhoto.setChatId(sendMediaGroup.getChatId());
        sendPhoto.setCaption(buf.toString());

        try {
            bot.execute(sendPhoto);
        } catch (TelegramApiException telegramApiException) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(sendMediaGroup.getChatId());
            sendMessage.setReplyToMessageId(sendMediaGroup.getReplyToMessageId());
            sendMessage.setText("Не удалось загрузить картинку по адресу: " + inputMedia.getMedia() + "\n" + buf);

            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                log.error("Still failed to send response: {}", e.getMessage());
            }
        }
    }
}
