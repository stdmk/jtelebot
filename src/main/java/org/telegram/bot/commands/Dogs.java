package org.telegram.bot.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
@Component
@Slf4j
public class Dogs implements Command<PartialBotApiMethod<?>> {

    private static final String DOGS_API_URL = "https://random.dog/woof.json";

    private final Bot bot;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    @Override
    public PartialBotApiMethod<?> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        Long chatId = message.getChatId();
        String textMessage = cutCommandInText(message.getText());

        if (textMessage != null) {
            return null;
        }
        bot.sendUploadPhoto(chatId);

        String imageUrl = getDogsImageUrl();
        InputFile inputFile = new InputFile(imageUrl);
        String commandName = "/" + this.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (imageUrl.toLowerCase().endsWith(".jpg")) {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setPhoto(inputFile);
            sendPhoto.setCaption(commandName);
            sendPhoto.setReplyToMessageId(message.getMessageId());
            sendPhoto.setChatId(chatId);

            return sendPhoto;
        }

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setCaption(commandName);
        sendDocument.setReplyToMessageId(message.getMessageId());
        sendDocument.setDocument(inputFile);

        return sendDocument;
    }

    private String getDogsImageUrl() {
        ResponseEntity<Dog> response;
        try {
            response = botRestTemplate.getForEntity(DOGS_API_URL, Dog.class);
        } catch (RestClientException e) {
            log.error(String.valueOf(e));
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return Optional.of(response)
                .map(HttpEntity::getBody)
                .map(Dog::getUrl)
                .orElseThrow(() -> new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE)));
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dog {
        private String url;
    }
}
