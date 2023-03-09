package org.telegram.bot.domain.commands;

import lombok.Data;
import lombok.RequiredArgsConstructor;
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
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.Serializable;

import static org.telegram.bot.utils.MathUtils.getRandomInRange;

@Component
@RequiredArgsConstructor
public class Butts implements CommandParent<SendPhoto> {

    private final Logger log = LoggerFactory.getLogger(Butts.class);

    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    private static final String BUTTS_API_URL = "http://api.obutts.ru/butts/";
    private static final String BUTTS_IMAGE_URL = "http://media.obutts.ru/butts/";

    public SendPhoto parse(Update update) throws BotException {
        Message message = getMessageFromUpdate(update);
        ResponseEntity<Butts.ButtsCount[]> response;

        try {
            response = botRestTemplate.getForEntity(BUTTS_API_URL + "count", Butts.ButtsCount[].class);
        } catch (RestClientException e) {
            log.error("Error receiving butts");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        Butts.ButtsCount[] buttsCounts = response.getBody();
        if (buttsCounts == null) {
            log.debug("Empty response from service");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        Integer numberOfPhoto = getRandomInRange(1, buttsCounts[0].getCount());
        String nameOfImage = String.format("%05d", numberOfPhoto) + ".jpg";

        String caption = "Butts";
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(new InputFile(BUTTS_IMAGE_URL + nameOfImage));
        sendPhoto.setCaption(caption);
        sendPhoto.setReplyToMessageId(message.getMessageId());
        sendPhoto.setChatId(message.getChatId().toString());
        sendPhoto.setHasSpoiler(true);

        return sendPhoto;
    }

    @Data
    private static class ButtsCount implements Serializable {
        private Integer count;
    }
}
