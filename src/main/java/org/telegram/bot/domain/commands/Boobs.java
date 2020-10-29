package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.InputStream;
import java.io.Serializable;

import static org.telegram.bot.utils.MathUtils.getRandomInRange;
import static org.telegram.bot.utils.NetworkUtils.getImageFromUrl;

@Component
@AllArgsConstructor
public class Boobs implements CommandParent<SendPhoto> {

    private final Logger log = LoggerFactory.getLogger(Boobs.class);

    private final SpeechService speechService;

    private static final String BOOBS_API_URL = "http://api.oboobs.ru/boobs/";
    private static final String BOOBS_IMAGE_URL = "http://media.oboobs.ru/boobs/";

    public SendPhoto parse(Update update) throws BotException {

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<BoobsCount[]> response = restTemplate.getForEntity(BOOBS_API_URL + "count", BoobsCount[].class);

        BoobsCount[] boobsCounts = response.getBody();
        if (boobsCounts == null) {
            log.debug("No response from service");
            throw new BotException(speechService.getRandomMessageByTag("noResponse"));
        }
        Integer numberOfPhoto = getRandomInRange(1, boobsCounts[0].getCount());

        String nameOfImage = String.format("%05d", numberOfPhoto) + ".jpg";
        InputStream boobs = getImageFromUrl(BOOBS_IMAGE_URL + nameOfImage);

        String caption = update.getMessage().getText();
        return new SendPhoto()
                .setPhoto(caption, boobs)
                .setCaption(caption)
                .setReplyToMessageId(update.getMessage().getMessageId())
                .setChatId(update.getMessage().getChatId());
    }

    @Data
    private static class BoobsCount implements Serializable {
        private Integer count;
    }
}
