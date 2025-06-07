package org.telegram.bot.services.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@RequiredArgsConstructor
@Service
@Slf4j
public class SendMediaGroupExecutor implements MethodExecutor {

    private final TelegramClient telegramClient;
    private final BotStats botStats;
    private final LanguageResolver languageResolver;
    private final InternationalizationService internationalizationService;

    @Override
    public String getMethod() {
        return SendMediaGroup.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, BotRequest request) {
        Message message = request.getMessage();
        SendMediaGroup sendMediaGroup = (SendMediaGroup) method;
        log.info("To {}: sending photos {}", message.getChatId(), sendMediaGroup);

        try {
            telegramClient.execute(sendMediaGroup);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(sendMediaGroup, e, "Failed to send media group");
            tryToSendOnePhoto(sendMediaGroup);
        } catch (Exception e) {
            botStats.incrementErrors(request, method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method) {
        SendMediaGroup sendMediaGroup = (SendMediaGroup) method;
        log.info("To {}: sending photos {}", sendMediaGroup.getChatId(), sendMediaGroup);

        try {
            telegramClient.execute(sendMediaGroup);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(sendMediaGroup, e, "Failed to send media group");
            tryToSendOnePhoto(sendMediaGroup);
        } catch (Exception e) {
            botStats.incrementErrors(method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }

    private void tryToSendOnePhoto(SendMediaGroup sendMediaGroup) {
        StringBuilder buf = new StringBuilder("${executor.sendmeadiagroup.otherpictures}: \n");
        sendMediaGroup.getMedias().stream().skip(1).forEach(inputMedia -> buf.append(inputMedia.getCaption()).append("\n"));

        InputMedia inputMedia = sendMediaGroup.getMedias().get(0);
        InputFile inputFile = new InputFile();
        inputFile.setMedia(inputMedia.getMedia());

        SendPhoto sendPhoto = new SendPhoto(sendMediaGroup.getChatId(), inputFile);
        sendPhoto.setPhoto(inputFile);
        sendPhoto.setReplyToMessageId(sendMediaGroup.getReplyToMessageId());
        sendPhoto.setCaption(buf.toString());

        internationalizationService.internationalize(sendPhoto, languageResolver.getChatLanguageCode(sendPhoto.getChatId()));

        try {
            telegramClient.execute(sendPhoto);
        } catch (TelegramApiException telegramApiException) {
            botStats.incrementErrors(sendMediaGroup, telegramApiException, "Failed to send media group");

            SendMessage sendMessage = new SendMessage(sendMediaGroup.getChatId(), "${executor.sendmeadiagroup.failedtodownload}: " + inputMedia.getMedia() + "\n" + buf);
            sendMessage.setReplyToMessageId(sendMediaGroup.getReplyToMessageId());

            internationalizationService.internationalize(sendMessage, languageResolver.getChatLanguageCode(sendPhoto.getChatId()));

            try {
                telegramClient.execute(sendMessage);
            } catch (TelegramApiException e) {
                botStats.incrementErrors(sendMediaGroup, e, "Failed to send media group");
                log.error("Still failed to send response: {}", e.getMessage());
            }
        }
    }
}
