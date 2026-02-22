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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.entities.GoogleSearchResult;
import org.telegram.bot.domain.entities.ImageUrl;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.TextUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class Google implements Command {

    private static final String SERP_API_URL = "https://serpapi.com/search.json?";

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
                    TextUtils.buildHtmlLink(googleSearchResult.getLink(), googleSearchResult.getFormattedUrl());

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
            SerpSearchData serpSearchData = getResultOfSearch(commandArgument, token);

            if (serpSearchData == null || serpSearchData.getOrganicResults() == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
            }

            List<GoogleSearchResult> googleSearchResults = serpSearchData.getOrganicResults()
                    .stream()
                    .map(result -> {
                        ImageUrl imageUrl = null;

                        if (result.getThumbnail() != null) {
                            imageUrl = imageUrlService.save(new ImageUrl()
                                    .setTitle(TextUtils.cutIfLongerThan(result.getTitle(), 255))
                                    .setUrl(result.getThumbnail()));
                        }

                        return new GoogleSearchResult()
                                .setTitle(result.getTitle())
                                .setLink(result.getLink())
                                .setDisplayLink(result.getDisplayedLink())
                                .setSnippet(result.getSnippet())
                                .setFormattedUrl(result.getLink())
                                .setImageUrl(imageUrl);
                    })
                    .collect(Collectors.toList());

            StringBuilder buf = new StringBuilder();
            googleSearchResultService.save(googleSearchResults).forEach(googleSearchResult ->
                    buf.append("<u>").append(TextUtils.buildHtmlLink(googleSearchResult.getLink(), googleSearchResult.getDisplayLink())).append("</u> ")
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
     * @param apiKey service access token.
     * @return google search data.
     */
    private SerpSearchData getResultOfSearch(String requestText, String apiKey) {
        ResponseEntity<SerpSearchData> response;

        try {
            response = botRestTemplate.getForEntity(
                    SERP_API_URL +
                            "engine=google" +
                            "&q=" + requestText +
                            "&json_restrictor=organic_results" +
                            "&api_key=" + apiKey,
                    SerpSearchData.class);
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
    public static class SerpSearchData {
        @JsonProperty("organic_results")
        private List<SerpOrganicResult> organicResults;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SerpOrganicResult {
        private Integer position;

        private String title;

        private String link;

        @JsonProperty("displayed_link")
        private String displayedLink;

        private String snippet;

        private String thumbnail;
    }

}
