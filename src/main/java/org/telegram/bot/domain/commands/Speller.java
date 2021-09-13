package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@AllArgsConstructor
public class Speller implements CommandParent<SendMessage> {

    private final RestTemplate botRestTemplate;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        Integer replyToMessage;
        String textMessage = getTextMessage(update);
        String responseText;

        if (textMessage == null) {
            textMessage = message.getReplyToMessage().getText();
            if (textMessage == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
            replyToMessage = message.getReplyToMessage().getMessageId();
        } else {
            replyToMessage = message.getMessageId();
        }

        responseText = getRevisedText(textMessage);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(replyToMessage);
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String getRevisedText(String text) throws BotException {
        List<SpellResult> spellResultList = getSpellerData(text);
        if (spellResultList.isEmpty()) {
            return "ошибок не обнаружено";
        }

        StringBuilder buf = new StringBuilder("<u>Найденные ошибки</u>\n");
        spellResultList.forEach(spellResult -> buf
                .append("<s>").append(spellResult.getWord()).append("</s> — ").append(spellResult.getS().get(0)).append("\n")
        );

        return buf.toString();
    }

    private List<SpellResult> getSpellerData(String text) throws BotException {
        final String SPELLER_API_URL = "https://speller.yandex.net/services/spellservice.json/checkText?text=";
        ResponseEntity<SpellResult[]> response;

        try {
            response = botRestTemplate.getForEntity(SPELLER_API_URL + text, SpellResult[].class);
        } catch (RestClientException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        SpellResult[] body = response.getBody();
        if (body == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(Arrays.asList(body));
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
