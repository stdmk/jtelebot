package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.ImageUrl;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GooglePics implements CommandParent<PartialBotApiMethod<?>> {

    private static final String GOOGLE_URL = "https://www.googleapis.com/customsearch/v1?searchType=image&";

    private final Bot bot;
    private final PropertiesConfig propertiesConfig;
    private final SpeechService speechService;
    private final ImageUrlService imageUrlService;
    private final CommandWaitingService commandWaitingService;
    private final RestTemplate botRestTemplate;
    private final BotStats botStats;
    private final NetworkUtils networkUtils;

    @Override
    public PartialBotApiMethod<?> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        if (textMessage == null) {
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.setText("теперь напиши мне что надо найти");

            return sendMessage;
        } else if (textMessage.startsWith("_")) {
            bot.sendUploadPhoto(message.getChatId());
            long imageId;
            try {
                imageId = Long.parseLong(textMessage.substring(1));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            log.debug("Request to get ImageUrl by id {}", imageId);
            ImageUrl imageUrl = imageUrlService.get(imageId);
            if (imageUrl == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            InputStream image;
            try {
                image = networkUtils.getFileFromUrlWithLimit(imageUrl.getUrl());
            } catch (Exception e) {
                log.debug("Error receiving ImageUrl {}", imageUrl);
                throw new BotException("Не удалось загрузить картинку по адресу: " + imageUrl.getUrl());
            }

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setPhoto(new InputFile(image, "google"));
            sendPhoto.setCaption(imageUrl.getTitle());
            sendPhoto.setParseMode("HTML");
            sendPhoto.setReplyToMessageId(message.getMessageId());
            sendPhoto.setChatId(message.getChatId().toString());

            return sendPhoto;

        } else {
            bot.sendUploadPhoto(message.getChatId());
            log.debug("Request to search images for {}", textMessage);
            List<InputMedia> images = new ArrayList<>();

            searchImagesOnGoogle(textMessage)
                    .forEach(imageUrl -> {
                        InputMediaPhoto inputMediaPhoto = new InputMediaPhoto();
                        inputMediaPhoto.setMedia(imageUrl.getUrl());
                        inputMediaPhoto.setCaption("/image_" + imageUrl.getId());

                        images.add(inputMediaPhoto);
                    });

            SendMediaGroup sendMediaGroup = new SendMediaGroup();
            sendMediaGroup.setMedias(images);
            sendMediaGroup.setReplyToMessageId(message.getMessageId());
            sendMediaGroup.setChatId(message.getChatId().toString());

            return sendMediaGroup;
        }
    }

    /**
     * Searching images in Google by text.
     *
     * @param text search text.
     * @return list of ImageUrl entities.
     * @see ImageUrl
     */
    public List<ImageUrl> searchImagesOnGoogle(String text) {
        String googleToken = propertiesConfig.getGoogleToken();
        if (googleToken == null || googleToken.equals("")) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        ResponseEntity<GooglePicsSearchData> response;
        try {
            response = botRestTemplate.getForEntity(GOOGLE_URL + "key=" + googleToken + "&q=" + text, GooglePicsSearchData.class);
        } catch (RestClientException e) {
            log.error("Error receiving result of searching images: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        botStats.incrementGoogleRequests();

        GooglePicsSearchData googlePicsSearchData = response.getBody();

        if (googlePicsSearchData == null || googlePicsSearchData.getItems() == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }

        return imageUrlService.save(googlePicsSearchData.getItems()
                .stream()
                .map(googlePicsSearchItem -> new ImageUrl()
                        .setTitle(googlePicsSearchItem.getTitle())
                        .setUrl(googlePicsSearchItem.getLink())
                )
                .collect(Collectors.toList()));
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GooglePicsSearchData {
        private List<GooglePicsSearchItem> items;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GooglePicsSearchItem {
        private String title;
        private String link;
    }
}
