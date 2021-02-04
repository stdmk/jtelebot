package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.ImageUrl;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ImageUrlService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static org.telegram.bot.utils.NetworkUtils.getFileFromUrl;

@Component
@AllArgsConstructor
public class Image implements CommandParent<SendPhoto> {

    ImageUrlService imageUrlService;
    private final SpeechService speechService;

    @Override
    public SendPhoto parse(Update update) throws Exception {
        Message message = getMessageFromUpdate(update);
        String textMessage = getTextMessage(update);
        ImageUrl imageUrl;

        if (textMessage == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        } else if (textMessage.startsWith("h")) {
            URL url;
            try {
                url = new URL(textMessage);
            } catch (MalformedURLException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            imageUrl = imageUrlService.get(url.toString());
            if (imageUrl == null) {
                imageUrl = new ImageUrl();
                imageUrl.setUrl(url.toString());
                imageUrl = imageUrlService.save(imageUrl);
            }
        } else {
            if (textMessage.startsWith("_")) {
                textMessage = textMessage.substring(1);
            }
            try {
                imageUrl = imageUrlService.get(Long.valueOf(textMessage));
            } catch (NumberFormatException numberFormatException) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }

        if (imageUrl == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        InputStream image;
        try {
            image = getFileFromUrl(imageUrl.getUrl());
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        SendPhoto sendPhoto = new SendPhoto();

        sendPhoto.setPhoto(new InputFile(image, imageUrl.getUrl()));
        sendPhoto.setCaption("/image_" + imageUrl.getId());
        sendPhoto.setReplyToMessageId(message.getMessageId());
        sendPhoto.setChatId(message.getChatId().toString());

        return sendPhoto;
    }
}
