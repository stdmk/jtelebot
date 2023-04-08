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
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.Serializable;

import static org.telegram.bot.utils.MathUtils.getRandomInRange;

@Component
@RequiredArgsConstructor
@Slf4j
public class Boobs implements CommandParent<SendPhoto> {

    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    private static final String BOOBS_API_URL = "http://api.oboobs.ru/boobs/";
    private static final String BOOBS_IMAGE_URL = "http://media.oboobs.ru/boobs/";

    public SendPhoto parse(Update update) throws BotException {
        Message message = getMessageFromUpdate(update);
        if (cutCommandInText(message.getText()) != null) {
            return null;
        }

        ResponseEntity<BoobsCount[]> response;
        try {
            response = botRestTemplate.getForEntity(BOOBS_API_URL + "count", BoobsCount[].class);
        } catch (RestClientException e) {
            log.error("Error receiving boobs");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        BoobsCount[] boobsCounts = response.getBody();
        if (boobsCounts == null) {
            log.debug("Empty response from service");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        Integer numberOfPhoto = getRandomInRange(1, boobsCounts[0].getCount());
        String nameOfImage = String.format("%05d", numberOfPhoto) + ".jpg";

        String caption = "Boobs";
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(new InputFile(BOOBS_IMAGE_URL + nameOfImage));
        sendPhoto.setCaption(caption);
        sendPhoto.setReplyToMessageId(message.getMessageId());
        sendPhoto.setChatId(message.getChatId().toString());
        sendPhoto.setHasSpoiler(true);

        return sendPhoto;
    }

    @Data
    protected static class BoobsCount implements Serializable {
        private Integer count;
    }
}
