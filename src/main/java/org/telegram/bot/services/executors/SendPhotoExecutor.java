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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@RequiredArgsConstructor
@Service
@Slf4j
public class SendPhotoExecutor implements MethodExecutor {

    private final TelegramClient telegramClient;
    private final BotStats botStats;
    private final LanguageResolver languageResolver;
    private final InternationalizationService internationalizationService;

    @Override
    public String getMethod() {
        return SendPhoto.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, BotRequest request) {
        Message message = request.getMessage();
        String lang = languageResolver.getChatLanguageCode(request);
        SendPhoto sendPhoto = internationalizationService.internationalize((SendPhoto) method, lang);
        log.info("To {}: sending photo {}", message.getChatId(), sendPhoto.getCaption());

        try {
            telegramClient.execute(sendPhoto);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(request, method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
            tryToDeliverTheMessage(sendPhoto);
        } catch (Exception e) {
            botStats.incrementErrors(request, method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method) {
        SendPhoto sendPhoto = (SendPhoto) method;

        String chatId = sendPhoto.getChatId();
        String lang = languageResolver.getChatLanguageCode(chatId);

        sendPhoto = internationalizationService.internationalize(sendPhoto, lang);

        log.info("To {}: sending photo {}", chatId, sendPhoto.getCaption());

        try {
            telegramClient.execute(sendPhoto);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
            tryToDeliverTheMessage(sendPhoto);
        } catch (Exception e) {
            botStats.incrementErrors(method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }

    private void tryToDeliverTheMessage(SendPhoto sendPhoto) {
        String imageUrl = sendPhoto.getPhoto().getAttachName();

        SendMessage sendMessage = new SendMessage(sendPhoto.getChatId(), "${executor.sendphoto.failedtosend}: " + imageUrl + "\n" + sendPhoto.getCaption());
        sendMessage.setReplyToMessageId(sendPhoto.getReplyToMessageId());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();

        internationalizationService.internationalize(sendMessage, languageResolver.getChatLanguageCode(sendPhoto.getChatId()));

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Still failed to send response: {}", e.getMessage());
        }
    }
}
