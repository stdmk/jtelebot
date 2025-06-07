package org.telegram.bot.commands;

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
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.entities.ImageUrl;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.ImageUrlService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GooglePics implements Command {

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
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        String commandArgument = commandWaitingService.getText(message);

        Long chatId = message.getChatId();
        if (commandArgument == null) {
            bot.sendTyping(chatId);
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());
            return returnResponse(new TextResponse(message)
                    .setText("${command.googlepics.commandwaitingstart}"));
        } else if (commandArgument.startsWith("_")) {
            bot.sendUploadPhoto(chatId);
            long imageId;
            try {
                imageId = Long.parseLong(commandArgument.substring(1));
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
                throw new BotException("${command.googlepics.unabletodownload}: " + imageUrl.getUrl());
            }

            return returnResponse(new FileResponse(message)
                    .addFile(new File(FileType.IMAGE, image, "google"))
                    .setText(imageUrl.getTitle())
                    .setResponseSettings(FormattingStyle.HTML));

        } else {
            bot.sendUploadPhoto(chatId);
            log.debug("Request to search images for {}", commandArgument);
            List<File> images = searchImagesOnGoogle(commandArgument)
                    .stream()
                    .map(imageUrl -> new File(FileType.IMAGE, imageUrl.getUrl(), "/image_" + imageUrl.getId()))
                    .toList();

            return returnResponse(new FileResponse(message)
                    .addFiles(images));
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
        if (googleToken == null || googleToken.isEmpty()) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        ResponseEntity<GooglePics.GooglePicsSearchData> response;
        try {
            response = botRestTemplate.getForEntity(GOOGLE_URL + "key=" + googleToken + "&q=" + text, GooglePicsSearchData.class);
        } catch (RestClientException e) {
            log.error("Error receiving result of searching images: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        botStats.incrementGoogleRequests();

        GooglePics.GooglePicsSearchData googlePicsSearchData = response.getBody();

        if (googlePicsSearchData == null || googlePicsSearchData.getItems() == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }

        return imageUrlService.save(googlePicsSearchData.getItems()
                .stream()
                .map(googlePicsSearchItem -> new ImageUrl()
                        .setTitle(googlePicsSearchItem.getTitle())
                        .setUrl(googlePicsSearchItem.getLink()))
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
