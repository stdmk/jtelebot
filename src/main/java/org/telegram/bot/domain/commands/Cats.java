package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@Slf4j
public class Cats implements CommandParent<PartialBotApiMethod<?>> {

    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    private static String CATS_API_URL = "https://api.thecatapi.com/v1/images/search";

    @Override
    public PartialBotApiMethod<?> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        if (textMessage != null) {
            return null;
        }

        ResponseEntity<Cat[]> response;
        try {
            response = botRestTemplate.getForEntity(CATS_API_URL, Cat[].class);
        } catch (RestClientException e) {
            log.error("Error receiving cats picture: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        Cat[] cats = response.getBody();
        if (cats == null) {
            log.debug("Empty response from service");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        Cat cat = cats[0];
        String url = cat.getUrl();
        if (url.endsWith(".gif")) {
            log.debug("The response is a gif");
            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(message.getChatId().toString());
            sendDocument.setReplyToMessageId(message.getMessageId());
            sendDocument.setDocument(new InputFile(url));

            return sendDocument;
        }

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(new InputFile(url));
        sendPhoto.setCaption("cats");
        sendPhoto.setReplyToMessageId(message.getMessageId());
        sendPhoto.setChatId(message.getChatId().toString());

        return sendPhoto;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Cat {
        private String id;
        private String url;
    }
}
