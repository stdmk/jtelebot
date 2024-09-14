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
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TextUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Butts implements Command {

    private static final String RANDOM_BUTTS_API_TEMPLATE = "http://api.obutts.ru/butts/0/%s/random";
    private static final String BUTTS_IMAGE_URL = "http://media.obutts.ru/butts/";
    private static final String BUTTS_SEARCHING_URL = "http://api.obutts.ru/butts/model/";
    private static final int RESPONSE_BUTTS_IMAGES_DEFAULT_COUNT = 1;
    private static final int RESPONSE_BUTTS_IMAGES_MAX_COUNT = 10;

    private final Bot bot;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    public List<BotResponse> parse(BotRequest request) throws BotException {
        Message message = request.getMessage();
        bot.sendUploadPhoto(message.getChatId());

        List<Butts.ButtsData> buttsDataList;
        String commandArgument = message.getCommandArgument();
        if (commandArgument != null) {
            if (TextUtils.isThatPositiveInteger(commandArgument)) {
                int buttsCount;
                try {
                    buttsCount = Integer.parseInt(commandArgument);
                } catch (NumberFormatException e) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                if (buttsCount < 1) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                buttsDataList = getRandomButts(buttsCount);
            } else {
                buttsDataList = searchingForButts(commandArgument);
            }
        } else {
            buttsDataList = getRandomButts(RESPONSE_BUTTS_IMAGES_DEFAULT_COUNT);
        }

        return returnResponse(new FileResponse(message)
                .setText("18+")
                .addFiles(buttsDataList.stream().map(this::mapToFile).toList()));
    }

    private File mapToFile(Butts.ButtsData buttsData) {
        String model = buttsData.getModel();
        if (model == null) {
            model = "";
        }
        String imageName = buttsData.getPreview().substring(buttsData.getPreview().indexOf("butts_preview/") + 14);

        return new File(FileType.IMAGE, BUTTS_IMAGE_URL + imageName, model, new FileSettings().setSpoiler(true));
    }

    private List<Butts.ButtsData> getRandomButts(int count) {
        if (count > RESPONSE_BUTTS_IMAGES_MAX_COUNT) {
            count = RESPONSE_BUTTS_IMAGES_MAX_COUNT;
        }

        Butts.ButtsData[] buttsData = getButtsData(String.format(RANDOM_BUTTS_API_TEMPLATE, count));
        if (buttsData.length == 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return Arrays.asList(buttsData);
    }

    private List<Butts.ButtsData> searchingForButts(String model) {
        Butts.ButtsData[] buttsData = getButtsData(BUTTS_SEARCHING_URL + model);
        if (buttsData.length == 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }

        return Arrays.stream(buttsData).limit(RESPONSE_BUTTS_IMAGES_MAX_COUNT).toList();
    }

    private Butts.ButtsData[] getButtsData(String url) {
        ResponseEntity<Butts.ButtsData[]> response;
        try {
            response = botRestTemplate.getForEntity(url, Butts.ButtsData[].class);
        } catch (RestClientException e) {
            log.error("Error receiving butts");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        Butts.ButtsData[] buttsData = response.getBody();
        if (buttsData == null) {
            log.debug("Empty response from service");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return buttsData;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ButtsData implements Serializable {
        private String model;
        private String preview;
        private String id;
        private Long rank;
        private String author;
    }
}
