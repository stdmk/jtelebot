package org.telegram.bot.domain.commands;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Speller implements CommandParent<SendMessage> {

    private final CommandWaitingService commandWaitingService;
    private final RestTemplate botRestTemplate;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        Integer replyToMessage;
        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        String responseText;
        if (textMessage == null) {
            Message repliedMessage = message.getReplyToMessage();
            if (repliedMessage == null) {
                commandWaitingService.add(message, this.getClass());
                responseText = "теперь напиши мне что нужно проверить";
                replyToMessage = message.getMessageId();
            } else {
                textMessage = repliedMessage.getText();
                if (textMessage == null) {
                    textMessage = repliedMessage.getCaption();
                    if (textMessage == null) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                    log.debug("Request to speller text from caption: {}", textMessage);
                    responseText = getRevisedText(textMessage);
                    replyToMessage = repliedMessage.getMessageId();
                } else {
                    log.debug("Request to speller text from replied message {}", textMessage);
                    responseText = getRevisedText(textMessage);
                    replyToMessage = message.getReplyToMessage().getMessageId();
                }
            }
        } else {
            replyToMessage = message.getMessageId();
            responseText = getRevisedText(textMessage);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(replyToMessage);
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    /**
     * Sending text to Speller for review.
     *
     * @param text for review
     * @return revised text
     */
    private String getRevisedText(String text) {
        final String SPELLER_API_URL = "https://speller.yandex.net/services/spellservice.json/checkText?text=";
        ResponseEntity<SpellResult[]> response;

        try {
            response = botRestTemplate.getForEntity(SPELLER_API_URL + text, SpellResult[].class);
        } catch (RestClientException e) {
            log.error("Error getting Speller review: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        SpellResult[] body = response.getBody();
        if (body == null || body.length == 0) {
            return "ошибок не обнаружено";
        }

        StringBuilder buf = new StringBuilder("<u>Найденные ошибки</u>\n");
        Arrays.asList(body).forEach(spellResult -> buf
                .append("<s>").append(spellResult.getWord()).append("</s> — ").append(spellResult.getS().get(0)).append("\n")
        );

        return buf.toString();
    }

    @Data
    private static class SpellResult {
        private Integer code;
        private Integer pos;
        private Integer row;
        private Integer col;
        private Integer len;
        private String word;
        private List<String> s;
    }
}
