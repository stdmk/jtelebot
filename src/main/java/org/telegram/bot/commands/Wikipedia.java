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
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.Wiki;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.WikiService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.reduceSpaces;
import static org.telegram.bot.utils.TextUtils.cutHtmlTags;

@Component
@RequiredArgsConstructor
@Slf4j
public class Wikipedia implements Command<SendMessage> {

    private static final String WIKI_SEARCH_URL = "https://ru.wikipedia.org/w/api.php?format=json&action=opensearch&search=";

    private final Bot bot;
    private final WikiService wikiService;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String responseText;
        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        if (textMessage == null) {
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.wikipedia.commandwaitingstart}";
        } else if (textMessage.startsWith("_")) {
            int wikiPageId;
            try {
                wikiPageId = Integer.parseInt(textMessage.substring(1));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            log.debug("Request to get wiki details by pageId {}", wikiPageId);
            Wiki wiki = wikiService.get(wikiPageId);
            if (wiki == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            responseText = getWikiPageDetails(wiki);
        } else {
            log.debug("Request to search wiki pages by text {}", textMessage);
            Wiki wiki = getWiki(textMessage);

            if (wiki != null && !wiki.getText().equals("")) {
                responseText = getWikiPageDetails(wiki);
            } else {
                List<String> titles = searchPageTitles(textMessage);

                if (titles.isEmpty()) {
                    responseText = speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
                } else if (titles.size() == 1) {
                    Wiki wiki1 = getWiki(titles.get(0));
                    if (wiki1 == null || wiki1.getText().equals("")) {
                        responseText = speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
                    } else {
                        responseText = getWikiPageDetails(wiki1);
                    }
                } else {
                    responseText = "<b>${command.wikipedia.searchresults} " + textMessage + "</b>\n" + buildSearchResponseText(titles);
                }
            }
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);

        return sendMessage;
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
                    .replaceAll("\\+", "_") + "'>${command.wikipedia.articlelink}</a>\n";
    }

    /**
     * Getting search response text by page titles.
     *
     * @param titles titles of pages.
     * @return formatted text with list of pages.
     */
    private String buildSearchResponseText(List<String> titles) {
        StringBuilder buf = new StringBuilder();
        titles
                .stream()
                .map(this::getWiki)
                .filter(Objects::nonNull)
                .map(wikiService::save)
                .forEach(wiki -> buf.append(wiki.getTitle()).append("\n").append("/wiki_").append(wiki.getPageId()).append("\n"));

        return buf.toString();
    }

    /**
     * Getting list of found titles by text.
     *
     * @param searchText search text.
     * @return list of found titles.
     */
    private List<String> searchPageTitles(String searchText) {
        List<String> titles = new ArrayList<>();

        ResponseEntity<Object[]> response = botRestTemplate.getForEntity(WIKI_SEARCH_URL + searchText, Object[].class);
        Object[] responseBody = response.getBody();

        if (responseBody == null) {
            return titles;
        }

        if (responseBody[1] instanceof List) {
            titles = ((List<?>) responseBody[1])
                    .stream()
                    .map(item -> (String) item)
                    .collect(Collectors.toList());
        }

        return titles;
    }

    /**
     * Getting Wiki by title.
     *
     * @param title title of wiki.
     * @return Wiki entity.
     */
    private Wiki getWiki(String title) {
        final String WIKI_API_URL = "https://ru.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=";

        ResponseEntity<WikiData> response;
        try {
            response = botRestTemplate.getForEntity(WIKI_API_URL + title, WikiData.class);
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
