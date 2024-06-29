package org.telegram.bot.commands;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Wiki;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.WikiService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.telegram.bot.utils.TextUtils.cutHtmlTags;
import static org.telegram.bot.utils.TextUtils.reduceSpaces;

@Component
@RequiredArgsConstructor
@Slf4j
public class Wikipedia implements Command {

    private static final String WIKI_API_URL = "https://%s.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=";
    private static final String WIKI_SEARCH_URL = "https://%s.wikipedia.org/w/api.php?format=json&action=opensearch&search=";

    private final Bot bot;
    private final WikiService wikiService;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final LanguageResolver languageResolver;
    private final RestTemplate botRestTemplate;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());

        String commandArgument = commandWaitingService.getText(message);

        String responseText;
        if (commandArgument == null) {
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.wikipedia.commandwaitingstart}";
        } else if (commandArgument.startsWith("_")) {
            responseText = getWikiTextById(commandArgument.substring(1));
        } else {
            log.debug("Request to search wiki pages by text {}", commandArgument);

            String lang = languageResolver.getChatLanguageCode(request);
            Wiki wiki = getWiki(commandArgument, lang);

            if (wiki != null && !wiki.getText().isEmpty()) {
                responseText = getWikiPageDetails(wiki);
            } else {
                responseText = searchWiki(commandArgument, lang);
            }
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(new ResponseSettings()
                        .setFormattingStyle(FormattingStyle.HTML)
                        .setWebPagePreview(false)));
    }

    private String getWikiTextById(String id) {
        int wikiPageId;
        try {
            wikiPageId = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        log.debug("Request to get wiki details by pageId {}", wikiPageId);
        Wiki wiki = wikiService.get(wikiPageId);
        if (wiki == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return getWikiPageDetails(wiki);
    }

    private String searchWiki(String searchText, String lang) {
        List<String> titles = searchPageTitles(searchText, lang);

        if (titles.isEmpty()) {
            return speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
        } else if (titles.size() == 1) {
            Wiki wiki1 = getWiki(titles.get(0), lang);
            if (wiki1 == null || wiki1.getText().isEmpty()) {
                return speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
            } else {
                return getWikiPageDetails(wiki1);
            }
        } else {
            return "<b>${command.wikipedia.searchresults} " + searchText + "</b>\n" + buildSearchResponseText(titles, lang);
        }
    }

    /**
     * Getting wiki page details.
     *
     * @param wiki Wiki entity.
     * @return details of wiki page.
     */
    private String getWikiPageDetails(Wiki wiki) {
        return "<b>" + wiki.getTitle() + "</b>\n" +
                wiki.getText() + "\n" +
                "<a href='https://ru.wikipedia.org/wiki/" + URLEncoder.encode(wiki.getTitle(), StandardCharsets.UTF_8)
                    .replace("\\+", "_") + "'>${command.wikipedia.articlelink}</a>\n";
    }

    /**
     * Getting search response text by page titles.
     *
     * @param titles titles of pages.
     * @param lang language code.
     * @return formatted text with list of pages.
     */
    private String buildSearchResponseText(List<String> titles, String lang) {
        StringBuilder buf = new StringBuilder();
        titles
                .stream()
                .map(title -> getWiki(title, lang))
                .filter(Objects::nonNull)
                .map(wikiService::save)
                .forEach(wiki -> buf.append(wiki.getTitle()).append("\n").append("/wiki_").append(wiki.getPageId()).append("\n"));

        return buf.toString();
    }

    /**
     * Getting list of found titles by text.
     *
     * @param searchText search text.
     * @param lang language code.
     * @return list of found titles.
     */
    private List<String> searchPageTitles(String searchText, String lang) {
        List<String> titles = new ArrayList<>();

        String url = String.format(WIKI_SEARCH_URL, lang) + searchText;
        ResponseEntity<Object[]> response = botRestTemplate.getForEntity(url, Object[].class);
        Object[] responseBody = response.getBody();

        if (responseBody == null) {
            return titles;
        }

        if (responseBody[1] instanceof List) {
            titles = ((List<?>) responseBody[1])
                    .stream()
                    .map(String.class::cast)
                    .toList();
        }

        return titles;
    }

    /**
     * Getting Wiki by title.
     *
     * @param title title of wiki.
     * @param lang language code.
     * @return Wiki entity.
     */
    private Wiki getWiki(String title, String lang) {
        ResponseEntity<WikiData> response;
        String url = String.format(WIKI_API_URL, lang) + title;
        try {
            response = botRestTemplate.getForEntity(url, WikiData.class);
        } catch (RestClientException e) {
            return null;
        }
        WikiData wikiData = response.getBody();

        if (wikiData == null) {
            return null;
        }

        WikiPage wikiPage;
        try {
            wikiPage = wikiData.getQuery().getPages().getWikiPage();
        } catch (Exception e) {
            return null;
        }

        return new Wiki()
                .setPageId(wikiPage.getPageid())
                .setTitle(wikiPage.getTitle())
                .setText(cutHtmlTags(wikiPage.getExtract()));
    }

    @Data
    private static class WikiData {
        @JsonIgnore
        private Object batchcomplete;
        private WikiQuery query;
    }

    @Data
    private static class WikiQuery {
        @JsonIgnore
        private Object normalized;
        private WikiPages pages;
    }

    @Data
    private static class WikiPages {
        private WikiPage wikiPage;

        @JsonAnySetter
        public void setPage(String propertyKey, Map<String, Object> value) {
            if (this.wikiPage == null) {
                this.wikiPage = new WikiPage();
            }
            wikiPage.setPageid((Integer) value.get("pageid"));
            wikiPage.setNs((Integer) value.get("ns"));
            wikiPage.setTitle(value.get("title").toString());
            String text = reduceSpaces(value.get("extract").toString());
            if (text.length() > 2047) {
                text = text.substring(0, 2047);
            }
            wikiPage.setExtract(text);
        }
    }

    @Data
    private static class WikiPage {
        private Integer pageid;
        private Integer ns;
        private String title;
        private String extract;
    }
}
