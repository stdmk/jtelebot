package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.ImageUrl;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.ParseMode;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.services.config.PropertiesConfig;
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

import static org.telegram.bot.utils.NetworkUtils.getFileFromUrl;

@Component
@AllArgsConstructor
public class GooglePics implements CommandParent<PartialBotApiMethod<?>> {

    private final Logger log = LoggerFactory.getLogger(Google.class);

    private final PropertiesConfig propertiesConfig;
    private final SpeechService speechService;
    private final ImageUrlService imageUrlService;
    private final CommandWaitingService commandWaitingService;
    private final RestTemplate botRestTemplate;

    @Override
    public PartialBotApiMethod<?> parse(Update update) throws Exception {
        String token = propertiesConfig.getGoogleToken();
        if (token == null || token.equals("")) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        Message message = getMessageFromUpdate(update);
        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        if (textMessage == null) {
            log.debug("Empty params. Waiting to continue...");
            commandWaitingService.add(message, GooglePics.class);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.setText("теперь напиши мне что надо найти");

            return sendMessage;
        } else if (textMessage.startsWith("_")) {
            long imageId;
            try {
                imageId = Long.parseLong(textMessage.substring(1));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            ImageUrl imageUrl = imageUrlService.get(imageId);
            if (imageUrl == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            InputStream image;
            try {
                image = getFileFromUrl(imageUrl.getUrl(), 5000000);
            } catch (Exception e) {
                throw new BotException("Не удалось загрузить картинку по адресу: " + imageUrl.getUrl());
            }

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setPhoto(new InputFile(image, "google"));
            sendPhoto.setCaption(imageUrl.getTitle());
            sendPhoto.setParseMode(ParseMode.HTML.getValue());
            sendPhoto.setReplyToMessageId(message.getMessageId());
            sendPhoto.setChatId(message.getChatId().toString());

            return sendPhoto;

        } else {
            GooglePicsSearchData googlePicsSearchData = getResultOfSearch(textMessage, token);

            if (googlePicsSearchData.getItems() == null) {
                throw new BotException("Ничего не нашёл по такому запросу");
            }
            List<ImageUrl> imageUrlList = googlePicsSearchData.getItems()
                    .stream()
                    .map(googlePicsSearchItem -> {
                        ImageUrl imageUrl = new ImageUrl();
                        imageUrl.setTitle(googlePicsSearchItem.getTitle());
                        imageUrl.setUrl(googlePicsSearchItem.getLink());

                        return imageUrl;
                    })
                    .collect(Collectors.toList());

            List<InputMedia> images = new ArrayList<>();

            imageUrlService.save(imageUrlList)
                    .forEach(imageUrl -> {
                        InputMediaPhoto inputMediaPhoto = new InputMediaPhoto();
                        inputMediaPhoto.setMedia(imageUrl.getUrl());

                        images.add(inputMediaPhoto);
                    });

            SendMediaGroup sendMediaGroup = new SendMediaGroup();
            sendMediaGroup.setMedias(images);
            sendMediaGroup.setReplyToMessageId(message.getMessageId());
            sendMediaGroup.setChatId(message.getChatId().toString());

            return sendMediaGroup;
        }
    }

    private GooglePicsSearchData getResultOfSearch(String requestText, String googleToken) {
        String GOOGLE_URL = "https://www.googleapis.com/customsearch/v1?searchType=image&";
        ResponseEntity<GooglePicsSearchData> response = botRestTemplate.getForEntity(
                GOOGLE_URL + "key=" + googleToken + "&q=" + requestText, GooglePicsSearchData.class);

        return response.getBody();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GooglePicsSearchData {
        private List<GooglePicsSearchItem> items;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GooglePicsSearchItem {
        private String title;
        private String link;
    }
}
