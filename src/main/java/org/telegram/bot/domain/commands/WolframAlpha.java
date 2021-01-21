package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
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
@AllArgsConstructor
public class WolframAlpha implements CommandParent<SendMessage> {

    private final PropertiesConfig propertiesConfig;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final RestTemplate botRestTemplate;

    @Override
    public SendMessage parse(Update update) throws Exception {
        String token = propertiesConfig.getWolframAlphaToken();
        if (token == null || token.equals("")) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        Message message = getMessageFromUpdate(update);
        String responseText;
        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        if (textMessage == null) {
            commandWaitingService.add(message, WolframAlpha.class);
            responseText = "теперь напиши мне что надо найти";
        } else {
            responseText = getWolframAlphaSearchResult(token, textMessage);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String getWolframAlphaSearchResult(String token, String requestText) {
        final String WOLFRAM_ALPHA_API_URL = "http://api.wolframalpha.com/v2/query?output=json&includepodid=Result&";
        ResponseEntity<WolframAlphaData> response = botRestTemplate.getForEntity(
                WOLFRAM_ALPHA_API_URL + "appid=" + token + "&input=" + requestText, WolframAlphaData.class);

        if (response.getBody() == null || response.getBody().getQueryresult() == null || response.getBody().getQueryresult().getPods() == null) {
            return "ничего не нашёл по такому запросу";
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
