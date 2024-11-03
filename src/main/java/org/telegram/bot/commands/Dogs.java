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
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
@Component
@Slf4j
public class Dogs implements Command {

    private static final String DOGS_API_URL = "https://random.dog/woof.json";
    private static final List<String> PHOTO_EXTENSION_LIST = List.of("jpg", "jpeg", "png");
    private static final List<String> VIDEO_EXTENSION_LIST = List.of("webm", "mp4");

    private final Bot bot;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Long chatId = message.getChatId();
        String commandArgument = message.getCommandArgument();

        if (commandArgument != null) {
            return returnResponse();
        }

        String imageUrl = getDogsImageUrl();
        String commandName = "/" + this.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        FileType fileType;
        if (isPhoto(imageUrl)) {
            bot.sendUploadPhoto(chatId);
            fileType = FileType.IMAGE;
        } else if (isVideo(imageUrl)) {
            bot.sendUploadVideo(chatId);
            fileType = FileType.VIDEO;
        } else {
            bot.sendUploadDocument(chatId);
            fileType = FileType.FILE;
        }

        File file = new File(fileType, imageUrl);
        return returnResponse(new FileResponse(message)
                .setText(commandName)
                .addFile(file));
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

        return url.substring(indexOfLastDot + 1).toLowerCase(Locale.ROOT);
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
