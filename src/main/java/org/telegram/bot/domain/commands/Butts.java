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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;

import static org.telegram.bot.utils.MathUtils.getRandomInRange;

@Component
@AllArgsConstructor
public class Butts implements CommandParent<SendPhoto> {

    private final Logger log = LoggerFactory.getLogger(Butts.class);

    private final SpeechService speechService;

    private static final String BUTTS_API_URL = "http://api.obutts.ru/butts/";
    private static final String BUTTS_IMAGE_URL = "http://media.obutts.ru/butts/";

    public SendPhoto parse(Update update) throws BotException {

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Butts.ButtsCount[]> response = restTemplate.getForEntity(BUTTS_API_URL + "count", Butts.ButtsCount[].class);

        Butts.ButtsCount[] buttsCounts = response.getBody();
        if (buttsCounts == null) {
            log.debug("No response from service");
            throw new BotException(speechService.getRandomMessageByTag("noResponse"));
        }
        Integer numberOfPhoto = getRandomInRange(1, buttsCounts[0].getCount());

        String nameOfImage = String.format("%05d", numberOfPhoto) + ".jpg";
        byte[] imageBytes;
        try {
            imageBytes = restTemplate.getForObject(BUTTS_IMAGE_URL + nameOfImage, byte[].class);
        } catch (RestClientException e) {
            log.debug("No response from service");
            throw new BotException(speechService.getRandomMessageByTag("noResponse"));
        }

        if (imageBytes == null) {
            log.debug("No response from service");
            throw new BotException(speechService.getRandomMessageByTag("noResponse"));
        }
        InputStream butts = new ByteArrayInputStream(imageBytes);

        String caption = update.getMessage().getText();
        return new SendPhoto()
                .setPhoto(caption, butts)
                .setCaption(caption)
                .setReplyToMessageId(update.getMessage().getMessageId())
                .setChatId(update.getMessage().getChatId());
    }

    @Data
    private static class ButtsCount implements Serializable {
        private Integer count;
    }
}
