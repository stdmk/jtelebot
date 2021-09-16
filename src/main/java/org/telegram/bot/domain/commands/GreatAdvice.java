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
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@Slf4j
public class GreatAdvice implements CommandParent<SendMessage> {

    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    @Override
    public SendMessage parse(Update update) {
        if (getTextMessage(update) != null) {
            return null;
        }

        log.debug("Request to get great advice");
        final String API_URL = "http://fucking-great-advice.ru/api/random";

        ResponseEntity<FuckingGreatAdvice> response;
        try {
            response = botRestTemplate.getForEntity(API_URL, FuckingGreatAdvice.class);
        } catch (RestClientException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        FuckingGreatAdvice fuckingGreatAdvice = response.getBody();
        if (fuckingGreatAdvice == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        Message message = getMessageFromUpdate(update);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(fuckingGreatAdvice.getText());

        return sendMessage;
    }

    @Data
    private static class FuckingGreatAdvice {
        private Integer id;
        private String text;
        private Object sound;
    }
}
