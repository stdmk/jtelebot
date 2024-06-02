package org.telegram.bot.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.GoogleSearchResult;
import org.telegram.bot.domain.entities.ImageUrl;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.GoogleSearchResultService;
import org.telegram.bot.services.ImageUrlService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.config.PropertiesConfig;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class Google implements Command {

    private static final String GOOGLE_URL = "https://www.googleapis.com/customsearch/v1?";

    private final Bot bot;
    private final PropertiesConfig propertiesConfig;
    private final SpeechService speechService;
    private final ImageUrlService imageUrlService;
    private final GoogleSearchResultService googleSearchResultService;
    private final CommandWaitingService commandWaitingService;
    private final RestTemplate botRestTemplate;
    private final BotStats botStats;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Long chatId = message.getChatId();

        String token = propertiesConfig.getGoogleToken();
        if (StringUtils.isEmpty(token)) {
            bot.sendTyping(chatId);
            log.error("Unable to find google token");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        String commandArgument = commandWaitingService.getText(message);

        String responseText;
        if (commandArgument == null) {
            bot.sendTyping(chatId);
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.google.commandwaitingstart}";
        } else if (commandArgument.startsWith("_")) {
            bot.sendTyping(chatId);
            long googleResultSearchId;
            try {
                googleResultSearchId = Long.parseLong(commandArgument.substring(1));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            log.debug("Request to getting result of google by id {}", googleResultSearchId);
            GoogleSearchResult googleSearchResult = googleSearchResultService.get(googleResultSearchId);
            if (googleSearchResult == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            responseText = "<b>" + googleSearchResult.getTitle() + "</b>\n" +
                            googleSearchResult.getSnippet() + "\n" +
                            "<a href='" + googleSearchResult.getLink() + "'>" + googleSearchResult.getFormattedUrl() + "</a>\n";

            ImageUrl imageUrl = googleSearchResult.getImageUrl();
            if (imageUrl != null) {
                return returnResponse(new FileResponse(message)
                        .setText(responseText)
                        .addFile(new File(FileType.IMAGE, imageUrl.getUrl()))
                        .setResponseSettings(FormattingStyle.HTML));
            }
        } else {
            bot.sendTyping(chatId);
            log.debug("Request to get google results for: {}", commandArgument);
            GoogleSearchData googleSearchData = getResultOfSearch(commandArgument, token);

            if (googleSearchData.getItems() == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
            }

            List<GoogleSearchResult> googleSearchResults = googleSearchData.getItems()
                    .stream()
                    .map(googleSearchItem -> {
                        ImageUrl imageUrl = null;

                        List<CseImage> cseImageList = Optional.ofNullable(googleSearchItem.getPagemap())
                                .map(Pagemap::getCseImage)
                                .orElse(null);

                        if (!CollectionUtils.isEmpty(cseImageList)) {
                            imageUrl = imageUrlService.save(new ImageUrl()
                                    .setTitle(googleSearchItem.getTitle())
                                    .setUrl(cseImageList.get(0).getSrc()));
                        }

                        return new GoogleSearchResult()
                                .setTitle(googleSearchItem.getTitle())
                                .setLink(googleSearchItem.getLink())
                                .setDisplayLink(googleSearchItem.getDisplayLink())
                                .setSnippet(googleSearchItem.getSnippet())
                                .setFormattedUrl(googleSearchItem.getFormattedUrl())
                                .setImageUrl(imageUrl);
                    })
                    .collect(Collectors.toList());

            StringBuilder buf = new StringBuilder();
            googleSearchResultService.save(googleSearchResults).forEach(googleSearchResult ->
                    buf.append("<a href=\"").append(googleSearchResult.getLink()).append("\"><u>").append(googleSearchResult.getDisplayLink()).append("</u></a> ")
                            .append(googleSearchResult.getTitle())
                            .append("\n/google_").append(googleSearchResult.getId()).append("\n\n")
            );

            responseText = buf.toString();
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(new ResponseSettings()
                        .setWebPagePreview(false)
                        .setFormattingStyle(FormattingStyle.HTML)));
    }

    /**
     * Getting Google search results for request.
     *
     * @param requestText search text.
     * @param googleToken service access token.
     * @return google search data.
     */
    private GoogleSearchData getResultOfSearch(String requestText, String googleToken) {
        ResponseEntity<GoogleSearchData> response;

        try {
            response = botRestTemplate.getForEntity(GOOGLE_URL + "key=" + googleToken + "&q=" + requestText, GoogleSearchData.class);
        } catch (RestClientException e) {
            log.error("Error receiving result of searching: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        botStats.incrementGoogleRequests();

        return response.getBody();
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleSearchData {
        private SearchInformation searchInformation;
        private List<GoogleSearchItem> items;
    }

    @Data
    private static class SearchInformation {
        private Float searchTime;
        private String formattedSearchTime;
        private String totalResults;
        private String formattedTotalResults;
    }

    @Data
    @Accessors(chain = true)
    public static class GoogleSearchItem {
        private String kind;
        private String title;
        private String htmlTitle;
        private String link;
        private String displayLink;
        private String snippet;
        private String htmlSnippet;
        private String cacheId;
        private String formattedUrl;
        private String htmlFormattedUrl;
        private Pagemap pagemap;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagemap {
        @JsonProperty("cse_image")
        private List<CseImage> cseImage;
    }

    @Data
    @Accessors(chain = true)
    public static class CseImage {
        private String src;
    }
}
