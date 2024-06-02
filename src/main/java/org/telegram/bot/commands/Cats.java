package org.telegram.bot.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class Cats implements Command {

    private static final String CATS_API_URL = "https://api.thecatapi.com/v1/images/search";

    private final Bot bot;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Long chatId = message.getChatId();

        if (message.hasCommandArgument()) {
            return returnResponse();
        }
        bot.sendUploadPhoto(chatId);

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
        String commandName = "/" + this.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        FileType fileType;
        if (url.endsWith(".gif")) {
            log.debug("The response is a gif");
            fileType = FileType.FILE;
        } else {
            fileType = FileType.IMAGE;
        }

        File file = new File(fileType, url);

        return returnResponse(new FileResponse(message)
                .setText(commandName)
                .addFile(file));
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cat {
        private String url;
    }
}
