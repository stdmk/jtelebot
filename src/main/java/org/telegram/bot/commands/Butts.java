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
public class Butts implements Command {

    private final Bot bot;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    private static final String BUTTS_API_URL = "http://api.obutts.ru/butts/";
    private static final String BUTTS_IMAGE_URL = "http://media.obutts.ru/butts/";

    public List<BotResponse> parse(BotRequest request) throws BotException {
        Message message = request.getMessage();
        if (message.hasCommandArgument()) {
            return returnResponse();
        }

        bot.sendUploadPhoto(message.getChatId());

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

        return returnResponse(new FileResponse(message)
                .setText("18+")
                .addFile(new File(FileType.IMAGE, BUTTS_IMAGE_URL + nameOfImage, new FileSettings().setSpoiler(true))));
    }

    @Data
    public static class ButtsCount implements Serializable {
        private Integer count;
    }
}
