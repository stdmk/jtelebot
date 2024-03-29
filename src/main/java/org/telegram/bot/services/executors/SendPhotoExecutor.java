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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RequiredArgsConstructor
@Service
@Slf4j
public class SendPhotoExecutor implements MethodExecutor {

    private final Bot bot;
    private final BotStats botStats;
    private final LanguageResolver languageResolver;
    private final InternationalizationService internationalizationService;

    @Override
    public String getMethod() {
        return SendPhoto.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, Update update) {
        Message message = TelegramUtils.getMessage(update);
        String lang = languageResolver.getChatLanguageCode(update);
        SendPhoto sendPhoto = internationalizationService.internationalize((SendPhoto) method, lang);
        log.info("To " + message.getChatId() + ": sending photo " + sendPhoto.getCaption());

        try {
            bot.execute(sendPhoto);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(update, method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
            tryToDeliverTheMessage(sendPhoto);
        } catch (Exception e) {
            botStats.incrementErrors(update, method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method) {
        SendPhoto sendPhoto = (SendPhoto) method;

        String chatId = sendPhoto.getChatId();
        String lang = languageResolver.getChatLanguageCode(chatId);

        sendPhoto = internationalizationService.internationalize(sendPhoto, lang);

        log.info("To " + chatId + ": sending photo " + sendPhoto.getCaption());

        try {
            bot.execute(sendPhoto);
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
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyToMessageId(sendPhoto.getReplyToMessageId());
        sendMessage.setChatId(sendPhoto.getChatId());
        sendMessage.setText("${executor.sendphoto.failedtosend}: " + imageUrl + "\n" + sendPhoto.getCaption());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();

        internationalizationService.internationalize(sendMessage, languageResolver.getChatLanguageCode(sendPhoto.getChatId()));

        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Still failed to send response: {}", e.getMessage());
        }
    }
}
