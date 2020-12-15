package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.ImageUrl;
import org.telegram.bot.domain.enums.ParseModes;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
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
    private final GoogleSearchResultService googleSearchResultService;
    private final CommandWaitingService commandWaitingService;

    @Override
    public PartialBotApiMethod<?> parse(Update update) throws Exception {
        String token = propertiesConfig.getGoogleToken();
        if (token == null || token.equals("")) {
            throw new BotException(speechService.getRandomMessageByTag("unableToFindToken"));
        }

        Message message = getMessageFromUpdate(update);
        String textMessage;
        boolean deleteCommandWaiting = false;

        CommandWaiting commandWaiting = commandWaitingService.get(message.getChatId(), message.getFrom().getId());
        if (commandWaiting == null) {
            textMessage = cutCommandInText(message.getText());
        } else {
            textMessage = message.getText();
            deleteCommandWaiting = true;
        }

        if (textMessage == null) {
            log.debug("Empty params. Waiting to continue...");
            commandWaiting = commandWaitingService.get(message.getChatId(), message.getFrom().getId());
            if (commandWaiting == null) {
                commandWaiting = new CommandWaiting();
                commandWaiting.setChatId(message.getChatId());
                commandWaiting.setUserId(message.getFrom().getId());
            }
            commandWaiting.setCommandName("picture");
            commandWaiting.setIsFinished(false);
            commandWaiting.setTextMessage("/picture ");
            commandWaitingService.save(commandWaiting);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.enableMarkdown(true);
            sendMessage.setText("теперь напиши мне что надо найти");

            return sendMessage;
        } else if (textMessage.startsWith("_")) {
            long imageId;
            try {
                imageId = Long.parseLong(textMessage.substring(1));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
            }

            ImageUrl imageUrl = imageUrlService.get(imageId);
            if (imageUrl == null) {
                throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
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
            sendPhoto.setParseMode(ParseModes.HTML.getValue());
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
                        InputStream image;
                        try {
                            image = getFileFromUrl(imageUrl.getUrl(), 5000000);
                        } catch (Exception e) {
                            return;
                        }

                        InputMediaPhoto inputMediaPhoto = new InputMediaPhoto();
                        inputMediaPhoto.setMedia(image, imageUrl.getTitle());

                        images.add(inputMediaPhoto);
                    });


            if (deleteCommandWaiting) {
                commandWaitingService.remove(commandWaiting);
            }

            SendMediaGroup sendMediaGroup = new SendMediaGroup();
            sendMediaGroup.setMedias(images);
            sendMediaGroup.setReplyToMessageId(message.getMessageId());
            sendMediaGroup.setChatId(message.getChatId().toString());

            return sendMediaGroup;
        }
    }

    private GooglePicsSearchData getResultOfSearch(String requestText, String googleToken) {
        RestTemplate restTemplate = new RestTemplate();
        String GOOGLE_URL = "https://www.googleapis.com/customsearch/v1?searchType=image&";
        ResponseEntity<GooglePicsSearchData> response = restTemplate.getForEntity(
                GOOGLE_URL + "key=" + googleToken + "&q=" + requestText, GooglePicsSearchData.class);

        return response.getBody();
    }

    @Data
    private static class GooglePicsSearchData {
        @JsonIgnore
        private String kind;

        @JsonIgnore
        private String url;

        @JsonIgnore
        private String queries;

        @JsonIgnore
        private String context;

        @JsonIgnore
        private String searchInformation;

        private List<GooglePicsSearchItem> items;
    }

    @Data
    private static class GooglePicsSearchItem {
        @JsonIgnore
        private String kind;

        private String title;

        @JsonIgnore
        private String htmlTitle;

        private String link;

        @JsonIgnore
        private String displayLink;

        @JsonIgnore
        private String snippet;

        @JsonIgnore
        private String htmlSnippet;

        @JsonIgnore
        private String mime;

        @JsonIgnore
        private String fileFormat;

        @JsonIgnore
        private Object image;
    }
}
