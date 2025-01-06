package org.telegram.bot.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WolframAlpha implements Command {

    private static final String WOLFRAM_ALPHA_API_URL = "http://api.wolframalpha.com/v2/query?output=json&includepodid=Result&";

    private final Bot bot;
    private final PropertiesConfig propertiesConfig;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final RestTemplate botRestTemplate;
    private final BotStats botStats;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        String token = propertiesConfig.getWolframAlphaToken();
        if (StringUtils.isEmpty(token)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());

        String commandArgument = commandWaitingService.getText(message);

        String responseText;
        if (commandArgument == null) {
            log.debug("Empty request. Enabling command waiting");
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.wolframalpfa.commandwaitingstart}";
        } else {
            log.debug("Request to get wolfram alpha for text {}", commandArgument);
            responseText = getWolframAlphaSearchResult(commandArgument, token);
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText));
    }

    private String getWolframAlphaSearchResult(String requestText, String token) {
        ResponseEntity<WolframAlphaData> response;
        try {
            response = botRestTemplate.getForEntity(
                    WOLFRAM_ALPHA_API_URL + "appid=" + token + "&input=" + requestText, WolframAlphaData.class);
        } catch (RestClientException e) {
            log.error("Error from api:", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        botStats.incrementWorlframRequests();

        return Optional.of(response)
                .map(HttpEntity::getBody)
                .map(WolframAlphaData::getQueryresult)
                .map(QueryResult::getPods)
                .filter(pods -> !pods.isEmpty())
                .map(pods -> pods.get(0))
                .map(Pod::getSubpods)
                .filter(subPods -> !subPods.isEmpty())
                .map(subPods -> subPods.get(0))
                .map(SubPod::getPlaintext)
                .filter(text -> !text.isEmpty())
                .orElseGet(() -> speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
    }

    @Data
    @Accessors(chain = true)
    public static class WolframAlphaData {
        private QueryResult queryresult;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Accessors(chain = true)
    public static class QueryResult {
        private List<Pod> pods;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Accessors(chain = true)
    public static class Pod {
        private List<SubPod> subpods;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Accessors(chain = true)
    public static class SubPod {
        private String plaintext;
    }
}
