package org.telegram.bot.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
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
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
@Component
@Slf4j
public class Dogs implements Command<PartialBotApiMethod<?>> {

    private static final String DOGS_API_URL = "https://random.dog/woof.json";
    private static final List<String> PHOTO_EXTENSION_LIST = List.of("jpg", "jpeg", "png");
    private static final List<String> VIDEO_EXTENSION_LIST = List.of("webm", "mp4");

    private final Bot bot;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    @Override
    public List<PartialBotApiMethod<?>> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        Long chatId = message.getChatId();
        String textMessage = cutCommandInText(message.getText());

        if (textMessage != null) {
            return Collections.emptyList();
        }

        String imageUrl = getDogsImageUrl();
        InputFile inputFile = new InputFile(imageUrl);
        String commandName = "/" + this.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (isPhoto(imageUrl)) {
            bot.sendUploadPhoto(chatId);

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setPhoto(inputFile);
            sendPhoto.setCaption(commandName);
            sendPhoto.setReplyToMessageId(message.getMessageId());
            sendPhoto.setChatId(chatId);

            return returnOneResult(sendPhoto);
        } else if (isVideo(imageUrl)) {
            bot.sendUploadVideo(chatId);

            SendVideo sendVideo = new SendVideo();
            sendVideo.setVideo(inputFile);
            sendVideo.setCaption(commandName);
            sendVideo.setReplyToMessageId(message.getMessageId());
            sendVideo.setChatId(chatId);

            return returnOneResult(sendVideo);
        }

        bot.sendUploadDocument(chatId);

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setCaption(commandName);
        sendDocument.setReplyToMessageId(message.getMessageId());
        sendDocument.setDocument(inputFile);

        return returnOneResult(sendDocument);
    }

    private boolean isPhoto(String url) {
        return PHOTO_EXTENSION_LIST.contains(getFileExtension(url));
    }

    private boolean isVideo(String url) {
        return VIDEO_EXTENSION_LIST.contains(getFileExtension(url));
    }

    private String getFileExtension(String url) {
        int indexOfLastDot = url.lastIndexOf(".");
        if (indexOfLastDot <= 0) {
            return "";
        }

        return url.substring(indexOfLastDot + 1).toLowerCase();
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
