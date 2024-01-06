package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.ImageUrl;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ImageUrlService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

@Component
@RequiredArgsConstructor
@Slf4j
public class Image implements Command<PartialBotApiMethod<?>> {

    private final Bot bot;
    private final ImageUrlService imageUrlService;
    private final SpeechService speechService;
    private final NetworkUtils networkUtils;
    private final GooglePics googlePics;

    @Override
    public PartialBotApiMethod<?> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = getTextMessage(update);
        ImageUrl imageUrl;

        if (textMessage == null) {
            log.debug("Request to get random image");
            imageUrl = imageUrlService.getRandom();
            if (imageUrl == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        } else if (textMessage.startsWith("http")) {
            log.debug("Request to get image from address {}", textMessage);

            URL url;
            try {
                url = new URL(textMessage);
            } catch (MalformedURLException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            imageUrl = imageUrlService.get(url.toString());
            if (imageUrl == null) {
                imageUrl = new ImageUrl().setUrl(url.toString());
                imageUrl = imageUrlService.save(imageUrl);
            }
        } else if (textMessage.startsWith("_")) {
            textMessage = textMessage.substring(1);

            log.debug("Request to get image by id {}", textMessage);
            try {
                imageUrl = imageUrlService.get(Long.valueOf(textMessage));
            } catch (NumberFormatException numberFormatException) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        } else {
            log.debug("Request to search image by text {}", textMessage);
            imageUrl = googlePics.searchImagesOnGoogle(textMessage).get(0);
        }

        if (imageUrl == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        InputStream image;
        Long imageId = imageUrl.getId();
        try {
            image = networkUtils.getFileFromUrlWithLimit(imageUrl.getUrl());
        } catch (Exception e) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setText("${command.image.failedtodownload}: " + imageUrl.getUrl() +
                    "\n" + Emoji.LEFT_ARROW.getSymbol() + " /image_" + (imageId - 1) +
                    "\n\n" + getNextImageCommandText(imageId + 1));
            sendMessage.enableHtml(true);
            sendMessage.disableWebPagePreview();

            return sendMessage;
        }

        SendPhoto sendPhoto = new SendPhoto();
        String caption = "\n";

        if (imageId > 1) {
            caption = caption + Emoji.LEFT_ARROW.getSymbol() + " /image_" + (imageId - 1) + "\n\n";
        }
        caption = caption + "/image_" + imageId + "\n\n" + getNextImageCommandText(imageId + 1);

        sendPhoto.setPhoto(new InputFile(image, imageUrl.getUrl()));
        sendPhoto.setCaption(caption);
        sendPhoto.setReplyToMessageId(message.getMessageId());
        sendPhoto.setChatId(message.getChatId().toString());

        return sendPhoto;
    }

    private String getNextImageCommandText(Long imageId) {
        if (imageUrlService.isImageUrlExists(imageId)) {
            return Emoji.RIGHT_ARROW.getSymbol() + " /image_" + imageId;
        } else {
            return "";
        }
    }
}
