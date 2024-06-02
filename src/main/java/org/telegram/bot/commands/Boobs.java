package org.telegram.bot.commands;

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
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import java.io.Serializable;
import java.util.List;

import static org.telegram.bot.utils.MathUtils.getRandomInRange;

@Component
@RequiredArgsConstructor
@Slf4j
public class Boobs implements Command {

    private final Bot bot;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    private static final String BOOBS_API_URL = "http://api.oboobs.ru/boobs/";
    private static final String BOOBS_IMAGE_URL = "http://media.oboobs.ru/boobs/";

    public List<BotResponse> parse(BotRequest request) throws BotException {
        Message message = request.getMessage();
        if (message.hasCommandArgument()) {
            return returnResponse();
        }

        bot.sendUploadPhoto(message.getChatId());

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

        return returnResponse(new FileResponse(message)
                .setText("18+")
                .addFile(new File(FileType.IMAGE, BOOBS_IMAGE_URL + nameOfImage, new FileSettings().setSpoiler(true))));
    }

    @Data
    public static class BoobsCount implements Serializable {
        private Integer count;
    }
}
