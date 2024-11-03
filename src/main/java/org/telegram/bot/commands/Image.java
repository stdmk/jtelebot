package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.ImageUrl;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ImageUrlService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Image implements Command {

    private final Bot bot;
    private final ImageUrlService imageUrlService;
    private final SpeechService speechService;
    private final NetworkUtils networkUtils;
    private final GooglePics googlePics;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendUploadPhoto(message.getChatId());
        String commandArgument = message.getCommandArgument();
        ImageUrl imageUrl;

        if (commandArgument == null) {
            log.debug("Request to get random image");
            imageUrl = imageUrlService.getRandom();
            if (imageUrl == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        } else if (commandArgument.startsWith("http")) {
            log.debug("Request to get image from address {}", commandArgument);

            URL url;
            try {
                url = new URL(commandArgument);
            } catch (MalformedURLException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            imageUrl = imageUrlService.get(url.toString());
            if (imageUrl == null) {
                imageUrl = new ImageUrl().setUrl(url.toString());
                imageUrl = imageUrlService.save(imageUrl);
            }
        } else if (commandArgument.startsWith("_")) {
            commandArgument = commandArgument.substring(1);

            log.debug("Request to get image by id {}", commandArgument);
            try {
                imageUrl = imageUrlService.get(Long.valueOf(commandArgument));
            } catch (NumberFormatException numberFormatException) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        } else {
            log.debug("Request to search image by text {}", commandArgument);
            imageUrl = googlePics.searchImagesOnGoogle(commandArgument).get(0);
        }

        if (imageUrl == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        InputStream image;
        Long imageId = imageUrl.getId();
        try {
            image = networkUtils.getFileFromUrlWithLimit(imageUrl.getUrl());
        } catch (Exception e) {
            return returnResponse(new TextResponse(message)
                    .setText("${command.image.failedtodownload}: " + imageUrl.getUrl() +
                            "\n" + Emoji.LEFT_ARROW.getSymbol() + " /image_" + (imageId - 1) +
                            "\n\n" + getNextImageCommandText(imageId + 1))
                    .setResponseSettings(new ResponseSettings()
                            .setFormattingStyle(FormattingStyle.HTML)
                            .setWebPagePreview(false)));
        }

        String caption = "\n";

        if (imageId > 1) {
            caption = caption + Emoji.LEFT_ARROW.getSymbol() + " /image_" + (imageId - 1) + "\n\n";
        }
        caption = caption + "/image_" + imageId + "\n\n" + getNextImageCommandText(imageId + 1);

        return returnResponse(new FileResponse(message)
                .addFile(new File(FileType.IMAGE, image, imageUrl.getUrl()))
                .setText(caption));
    }

    private String getNextImageCommandText(Long imageId) {
        if (imageUrlService.isImageUrlExists(imageId)) {
            return Emoji.RIGHT_ARROW.getSymbol() + " /image_" + imageId;
        } else {
            return "";
        }
    }
}
