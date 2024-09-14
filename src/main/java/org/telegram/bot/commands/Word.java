package org.telegram.bot.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class Word implements Command {

    private static final String API_URL = "https://%s.wiktionary.org/w/api.php?action=query&prop=extracts&format=json&explaintext=&titles=";
    private static final String SITE_URL = "https://ru.wiktionary.org/wiki/";
    private static final Pattern TITLE_PATTERN = Pattern.compile("=+ ([\\w ]+) =+", Pattern.UNICODE_CHARACTER_CLASS);

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final LanguageResolver languageResolver;
    private final RestTemplate botRestTemplate;
    private final SpeechService speechService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());

        String commandArgument = commandWaitingService.getText(message);

        List<String> responseText;
        if (commandArgument == null) {
            commandWaitingService.add(message, this.getClass());
            responseText = List.of("${command.word.commandwaitingstart}");
        } else {
            String lang = languageResolver.getChatLanguageCode(request);
            responseText = getData(commandArgument, lang);
            responseText.add(TextUtils.buildLink(SITE_URL + commandArgument, "${command.word.gotosite}", true));
        }

        return mapToTextResponseList(responseText, message, new ResponseSettings().setFormattingStyle(FormattingStyle.HTML));
    }

    private List<String> getData(String title, String lang) {
        ResponseEntity<WiktionaryData> response;
        String url = String.format(API_URL, lang) + title;
        try {
            response = botRestTemplate.getForEntity(url, WiktionaryData.class);
        } catch (RestClientException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        Optional<String> optionalExtract = Optional.ofNullable(response.getBody())
                .map(WiktionaryData::getQuery)
                .map(Query::getPages)
                .map(map -> map.entrySet().iterator().next())
                .map(Map.Entry::getValue)
                .map(PageData::getExtract);

        if (optionalExtract.isEmpty()) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }

        String data = optionalExtract.get();
        if (data.isBlank()) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }

        return buildResponseText(data);
    }

    private List<String> buildResponseText(String data) {
        List<String> result = new ArrayList<>();

        data = TextUtils.removeDuplicateLineBreaks(data);
        data = TextUtils.cutHtmlTags(data);
        data = titlesToHtml(data);

        String[] strings = data.split("\n");
        int lastElementIndex = strings.length - 1;
        for (int i = 0; i < strings.length; i++) {
            if (stringHasNoData(strings[i])
                    || (strings[i].startsWith("<b>") && i == lastElementIndex)
                    || (i != lastElementIndex) && (strings[i].startsWith("<b>") && (strings[i + 1].startsWith("<b>") || stringHasNoData(strings[i + 1])))) {
                continue;
            }

            if (strings[i].startsWith("<b>")) {
                strings[i] = "\n" + strings[i];
            }

            result.add(strings[i] + "\n");
        }

        return result;
    }

    private boolean stringHasNoData(String string) {
        return string.isBlank()
                || ("—".equals(string))
                || ("-".equals(string))
                || ("?".equals(string));
    }

    private String titlesToHtml(String data) {
        Matcher matcher = TITLE_PATTERN.matcher(data);
        while (matcher.find()) {
            data = data.replace(matcher.group(), "<b>" + matcher.group(1) + "</b>");
        }

        return data;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WiktionaryData {
        @JsonProperty("batchcomplete")
        private String batchComplete;

        @JsonProperty("query")
        private Query query;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Query {
        @JsonProperty("pages")
        private Map<String, PageData> pages;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageData {
        @JsonProperty("pageid")
        private Integer pageid;

        @JsonProperty("ns")
        private Integer ns;

        @JsonProperty("title")
        private String title;

        @JsonProperty("extract")
        private String extract;
    }

}
