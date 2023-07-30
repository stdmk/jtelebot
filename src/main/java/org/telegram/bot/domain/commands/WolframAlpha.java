package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WolframAlpha implements CommandParent<SendMessage> {

    private final Bot bot;
    private final PropertiesConfig propertiesConfig;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final RestTemplate botRestTemplate;
    private final BotStats botStats;

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
            log.debug("Empty request. Enabling command waiting");
            commandWaitingService.add(message, this.getClass());
            responseText = "теперь напиши мне что надо найти";
        } else {
            log.debug("Request to get wolfram alpha for text {}", textMessage);
            responseText = getWolframAlphaSearchResult(textMessage);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(responseText);

        return sendMessage;
    }

    /**
     * Getting Wolfram alpha result.
     *
     * @param requestText text of request
     * @return result of wolfram alpha.
     */
    private String getWolframAlphaSearchResult(String requestText) {
        String token = propertiesConfig.getWolframAlphaToken();
        if (StringUtils.isEmpty(token)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        final String WOLFRAM_ALPHA_API_URL = "http://api.wolframalpha.com/v2/query?output=json&includepodid=Result&";

        ResponseEntity<WolframAlphaData> response;
        try {
            response = botRestTemplate.getForEntity(
                    WOLFRAM_ALPHA_API_URL + "appid=" + token + "&input=" + requestText, WolframAlphaData.class);
        } catch (RestClientException e) {
            log.error("Error from api:", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        botStats.incrementWorlframRequests();

        if (response.getBody() == null || response.getBody().getQueryresult() == null || response.getBody().getQueryresult().getPods() == null) {
            return speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
        }

        return response.getBody().getQueryresult().getPods().get(0).getSubpods().get(0).getPlaintext();
    }

    @Data
    private static class WolframAlphaData {
        private QueryResult queryresult;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class QueryResult {
        private List<Pod> pods;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Pod {
        private List<SubPod> subpods;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SubPod {
        private String plaintext;
    }
}
