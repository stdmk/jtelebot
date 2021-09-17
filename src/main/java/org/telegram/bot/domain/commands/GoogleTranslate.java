package org.telegram.bot.domain.commands;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleTranslate implements CommandParent<SendMessage> {

    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final NetworkUtils networkUtils;
    private final RestTemplate botRestTemplate;
    private final PropertiesConfig propertiesConfig;

    private final List<Character> enAlphabet = "qwertyuiopasdfghjklzxcvbnm".chars().mapToObj(s -> (char) s).collect(Collectors.toList());

    @Override
    public SendMessage parse(Update update) {
        Integer replyToMessage;
        String responseText;

        Message message = getMessageFromUpdate(update);
        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        if (textMessage != null) {
            log.debug("Request to translate text from message: {}", textMessage);
            responseText = translateText(textMessage);
            replyToMessage = message.getMessageId();
        } else {
            if (message.getReplyToMessage() == null) {
                log.debug("Empty request. Turning on command waiting");

                commandWaitingService.add(message, this.getClass());

                replyToMessage = message.getMessageId();
                responseText = "теперь напиши мне что нужно перевести";
            } else {
                String requestText = message.getReplyToMessage().getText();
                if (requestText == null) {
                    requestText = message.getReplyToMessage().getCaption();
                    if (requestText == null) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                }

                responseText = translateText(requestText);
                replyToMessage = message.getReplyToMessage().getMessageId();
            }
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(replyToMessage);
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String translateText(String requestText) {
        String token = propertiesConfig.getGoogleTranslateToken();
        if (StringUtils.isEmpty(token)) {
            log.error("Unable to find google translate token");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }
        final String GOOGLE_TRANSLATE_URL = "https://script.google.com/macros/s/" + token + "/exec?";

        String targetLang;
        String sourceLang;
        if (enAlphabet.contains(requestText.charAt(0))) {
            sourceLang = "en";
            targetLang = "ru";
        } else {
            sourceLang = "ru";
            targetLang = "en";
        }

        ResponseEntity<TranslateResult> response;
        try {
            response = botRestTemplate.getForEntity(GOOGLE_TRANSLATE_URL +
                    "q=" + requestText + "&target=" + targetLang + "&source=" + sourceLang, TranslateResult.class);
        } catch (RestClientException e) {
            log.error("Error receiving result of searching: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        if (response.getBody() == null) {
            log.error("Empty result of translate");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return response.getBody().getText();
    }

    @Data
    private static class TranslateResult {
        private String text;
    }
}
