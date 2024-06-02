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
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TextUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GreatAdvice implements Command {

    private static final String API_URL = "http://fucking-great-advice.ru/api/random";

    private final Bot bot;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        if (message.hasCommandArgument()) {
            return returnResponse();
        }

        bot.sendTyping(message.getChatId());
        log.debug("Request to get great advice");

        ResponseEntity<FuckingGreatAdvice> response;
        try {
            response = botRestTemplate.getForEntity(API_URL, FuckingGreatAdvice.class);
        } catch (RestClientException e) {
            log.error("Error from api:", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        FuckingGreatAdvice fuckingGreatAdvice = response.getBody();
        if (fuckingGreatAdvice == null) {
            log.error("Empty response from FGA api");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return returnResponse(new TextResponse(message)
                .setText("18+\n" + TextUtils.wrapTextToSpoiler(fuckingGreatAdvice.getText()))
                .setResponseSettings(FormattingStyle.HTML));
    }

    @Data
    public static class FuckingGreatAdvice {
        private Integer id;
        private String text;
        private Object sound;
    }
}
