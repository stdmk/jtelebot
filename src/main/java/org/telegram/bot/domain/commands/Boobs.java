package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
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
@AllArgsConstructor
public class Boobs implements CommandParent<SendPhoto> {

    private final Logger log = LoggerFactory.getLogger(Boobs.class);

    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    private static final String BOOBS_API_URL = "http://api.oboobs.ru/boobs/";
    private static final String BOOBS_IMAGE_URL = "http://media.oboobs.ru/boobs/";

    public SendPhoto parse(Update update) throws BotException {
        Message message = getMessageFromUpdate(update);

        ResponseEntity<BoobsCount[]> response = botRestTemplate.getForEntity(BOOBS_API_URL + "count", BoobsCount[].class);

        BoobsCount[] boobsCounts = response.getBody();
        if (boobsCounts == null) {
            log.debug("No response from service");
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

        return sendPhoto;
    }

    @Data
    private static class BoobsCount implements Serializable {
        private Integer count;
    }
}
