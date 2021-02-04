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

@Component
@AllArgsConstructor
public class GreatAdvice implements CommandParent<SendMessage> {

    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    @Override
    public SendMessage parse(Update update) throws Exception {
        if (getTextMessage(update) != null) {
            return null;
        }

        final String API_URL = "http://fucking-great-advice.ru/api/random";

        ResponseEntity<FuckingGreateAdvice> response;
        try {
            response = botRestTemplate.getForEntity(API_URL, FuckingGreateAdvice.class);
        } catch (RestClientException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        FuckingGreateAdvice fuckingGreateAdvice = response.getBody();
        if (fuckingGreateAdvice == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        Message message = getMessageFromUpdate(update);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(fuckingGreateAdvice.getText());

        return sendMessage;
    }

    @Data
    private static class FuckingGreateAdvice {
        private Integer id;
        private String text;
        private Object sound;
    }
}
