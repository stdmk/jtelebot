package org.telegram.bot.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TextUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class Steam implements Command {

    private static final String SEARCH_API_URL_TEMPLATE = "https://store.steampowered.com/api/storesearch?cc=%s&term=%s";
    private static final String DETAILS_API_URL_TEMPLATE = "https://store.steampowered.com/api/appdetails?cc=%s&l=%s&appids=%s";
    private static final String STORE_URL = "https://store.steampowered.com/app/";
    private static final ResponseSettings DEFAULT_RESPONSE_SETTINGS = new ResponseSettings()
            .setFormattingStyle(FormattingStyle.HTML)
            .setWebPagePreview(false);

    private final CommandWaitingService commandWaitingService;
    private final LanguageResolver languageResolver;
    private final RestTemplate botRestTemplate;
    private final SpeechService speechService;
    private final ObjectMapper objectMapper;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        String commandArgument = commandWaitingService.getText(message);

        SteamResponse response;
        if (commandArgument == null) {
            commandWaitingService.add(message, this.getClass());
            response = new SteamResponse("${command.steam.commandwaitingstart}");
        } else {
            String lang = languageResolver.getChatLanguageCode(message, message.getUser());

            String potentialId;
            if (commandArgument.startsWith("_")) {
                potentialId = commandArgument.substring(1);
            } else {
                potentialId = commandArgument;
            }

            if (TextUtils.isThatPositiveInteger(potentialId)) {
                response = getAppDetails(Integer.parseInt(potentialId), lang);
            } else {
                response = searchApp(commandArgument, lang);
            }
        }

        if (response.hasImage()) {
            return returnResponse(new FileResponse(message)
                    .setText(response.getText())
                    .addFile(new File(FileType.IMAGE, response.getImageUrl()))
                    .setResponseSettings(DEFAULT_RESPONSE_SETTINGS));
        }

        return returnResponse(new TextResponse(message)
                .setText(response.getText())
                .setResponseSettings(DEFAULT_RESPONSE_SETTINGS));
    }

    private SteamResponse searchApp(String text, String lang) {
        String url = String.format(SEARCH_API_URL_TEMPLATE, lang, text);
        SearchResult searchResult = getSteamData(url, new ParameterizedTypeReference<>() {}, lang);
        return toSteamResponse(searchResult);
    }

    private SteamResponse toSteamResponse(SearchResult searchResult) {
        String responseText;
        if (searchResult == null || searchResult.getTotal() == null || searchResult.getTotal() == 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }

        responseText = searchResult.getItems().stream().map(this::buildString).collect(Collectors.joining("\n\n"));

        return new SteamResponse(responseText);
    }

    private String buildString(Item item) {
        return "<b>" + item.getName() + "</b>\n/steam_" + item.getId();
    }

    private SteamResponse getAppDetails(Integer appId, String lang) {
        DetailResult detailResult = getAppDetailsSteamData(appId, lang);
        if (detailResult != null && Boolean.FALSE.equals(detailResult.getSuccess())) { // may be "This product is not available in your region"
            detailResult = getAppDetailsSteamData(appId, "en");
        }

        return toSteamResponse(detailResult);
    }

    private SteamResponse toSteamResponse(DetailResult detailResult) {
        if (detailResult == null || !Boolean.TRUE.equals(detailResult.getSuccess()) || detailResult.getData() == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        StringBuilder buf = new StringBuilder();
        AppData appData = detailResult.getData();
        buf.append("<b><u>").append(appData.getName()).append("</u></b>\n");
        buf.append("ID: <code>").append(appData.getAppId()).append("</code>\n");
        buf.append("<b>").append(appData.getRequiredAge()).append("+</b>\n");
        Optional.of(appData).map(AppData::getReleaseDate).map(ReleaseDate::getDate).ifPresent(releaseDate ->
                buf.append("${command.steam.releasedate}: <b>").append(releaseDate).append("</b>\n"));
        buf.append("${command.steam.developers}: <b>").append(String.join(",", appData.getDevelopers())).append("</b>\n");
        Optional.of(appData).map(AppData::getPrice).map(Price::getFinalFormatted).ifPresent(price ->
                buf.append("${command.steam.price}: <b>").append(price).append("</b>\n"));
        buf.append(TextUtils.cutHtmlTags(appData.getDescription())).append("\n");
        buf.append(TextUtils.buildHtmlLink(STORE_URL + appData.getAppId(), "${command.steam.store}"));

        return new SteamResponse(buf.toString(), appData.getImageUrl());
    }

    private DetailResult getAppDetailsSteamData(Integer appId, String lang) {
        String url = String.format(DETAILS_API_URL_TEMPLATE, lang, lang, appId);
        Map<String, Object> steamData = getSteamData(url, new ParameterizedTypeReference<>() {}, lang);
        return objectMapper.convertValue(steamData.get(appId.toString()), DetailResult.class);
    }

    private <T> T getSteamData(String apiUrl, ParameterizedTypeReference<T> typeReference, String lang) throws BotException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAcceptLanguageAsLocales(List.of(Locale.forLanguageTag(lang)));

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<T> response;
        try {
            response = botRestTemplate.exchange(apiUrl, HttpMethod.GET, entity, typeReference);
        } catch (HttpClientErrorException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return response.getBody();
    }

    @Getter
    private static class SteamResponse {
        private final String text;
        private final String imageUrl;

        public SteamResponse(String text, String imgUrl) {
            this.text = text;
            this.imageUrl = imgUrl;
        }

        public SteamResponse(String text) {
            this.text = text;
            this.imageUrl = null;
        }

        public boolean hasImage() {
            return this.imageUrl != null;
        }
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResult {
        private Integer total;
        private List<Item> items;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String name;
        private Long id;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetailResult {
        private Boolean success;
        private AppData data;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AppData {
        private String name;

        @JsonProperty(value = "steam_appid")
        private Long appId;

        @JsonProperty(value = "required_age")
        private Integer requiredAge;

        @JsonProperty(value = "short_description")
        private String description;

        @JsonProperty(value = "header_image")
        private String imageUrl;

        private List<String> developers;

        @JsonProperty(value = "price_overview")
        private Price price;

        @JsonProperty(value = "release_date")
        private ReleaseDate releaseDate;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Price {
        @JsonProperty(value = "final_formatted")
        private String finalFormatted;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReleaseDate {
        private String date;
    }

}
