package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.Wiki;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.WikiService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.reduceSpaces;

@Component
@AllArgsConstructor
public class Wikipedia implements CommandParent<SendMessage> {

    private final Logger log = LoggerFactory.getLogger(Wikipedia.class);

    private final WikiService wikiService;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) throws Exception {
        Message message = getMessageFromUpdate(update);
        String textMessage;
        String responseText;
        boolean deleteCommandWaiting = false;

        CommandWaiting commandWaiting = commandWaitingService.get(message.getChatId(), message.getFrom().getId());
        if (commandWaiting == null) {
            textMessage = cutCommandInText(message.getText());
        } else {
            textMessage = message.getText();
            deleteCommandWaiting = true;
        }

        if (textMessage == null) {
            deleteCommandWaiting = false;
            log.debug("Empty params. Waiting to continue...");
            commandWaiting = commandWaitingService.get(message.getChatId(), message.getFrom().getId());
            if (commandWaiting == null) {
                commandWaiting = new CommandWaiting();
                commandWaiting.setChatId(message.getChatId());
                commandWaiting.setUserId(message.getFrom().getId());
            }
            commandWaiting.setCommandName("wiki");
            commandWaiting.setIsFinished(false);
            commandWaiting.setTextMessage("/wiki ");
            commandWaitingService.save(commandWaiting);

            responseText = "теперь напиши мне что надо найти";
        } else if (textMessage.startsWith("_")) {
            int wikiPageId;
            try {
                wikiPageId = Integer.parseInt(textMessage.substring(1));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
            }

            Wiki wiki = wikiService.get(wikiPageId);
            if (wiki == null) {
                throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
            }

            responseText = prepareSearchResponse(wiki);
        } else {
            Wiki wiki = getWiki(textMessage);
            if (wiki != null && !wiki.getText().equals("")) {
                responseText = prepareSearchResponse(wiki);
            } else {
                List<String> titles = searchPageTitles(textMessage);

                if (titles.isEmpty()) {
                    responseText = "ничего не нашёл по такому запросу";
                } else if (titles.size() == 1) {
                    Wiki wiki1 = getWiki(titles.get(0));
                    if (wiki1 == null || wiki1.getText().equals("")) {
                        responseText = "ничего не нашёл по такому запросу";
                    } else {
                        responseText = prepareSearchResponse(wiki1);
                    }
                } else {
                    responseText = "<b>Результаты по запросу " + textMessage + "</b>\n" + prepareSearchResponse(titles);
                }
            }
        }

        if (deleteCommandWaiting) {
            commandWaitingService.remove(commandWaiting);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String prepareSearchResponse(Wiki wiki) {
        return "<b>" + wiki.getTitle() + "</b>\n" +
                wiki.getText() + "\n" +
                "<a href='https://ru.wikipedia.org/wiki/" + wiki.getTitle() + "'>Ссылка на статью</a>\n";
    }

    private String prepareSearchResponse(List<String> titles) {
        StringBuilder buf = new StringBuilder();

        getListOfWikiPages(titles)
                .forEach(wiki -> buf.append(wiki.getTitle()).append("\n").append("/wiki_").append(wiki.getPageId()).append("\n\n"));

        return buf.toString();
    }

    private List<String> searchPageTitles(String searchText) {
        final String WIKI_SEARCH_URL = "https://ru.wikipedia.org/w/api.php?format=json&action=opensearch&search=";
        List<String> titles = new ArrayList<>();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Object[]> response = restTemplate.getForEntity(WIKI_SEARCH_URL + searchText, Object[].class);
        Object[] responseBody = response.getBody();

        if (responseBody == null) {
            return titles;
        }

        if (responseBody[1] instanceof List) {
            titles = ((List<?>) responseBody[1])
                    .stream()
                    .filter(item -> item instanceof String)
                    .map(item -> (String) item)
                    .collect(Collectors.toList());
        }

        return titles;
    }

    private List<Wiki> getListOfWikiPages(List<String> titles) {
        return wikiService.save(titles
                .stream()
                .map(this::getWiki)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    private Wiki getWiki(String title) {
        final String WIKI_API_URL = "https://ru.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=";
        Wiki wiki;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<WikiData> response;
        try {
            response = restTemplate.getForEntity(WIKI_API_URL + title, WikiData.class);
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

        wiki = new Wiki();
        wiki.setPageId(wikiPage.getPageid());
        wiki.setTitle(wikiPage.getTitle());
        wiki.setText(wikiPage.getExtract());

        return wiki;
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
