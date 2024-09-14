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
public class Boobs implements Command {

    private static final String RANDOM_BOOBS_API_TEMPLATE = "http://api.oboobs.ru/boobs/0/%s/random";
    private static final String BOOBS_IMAGE_URL = "http://media.oboobs.ru/boobs/";
    private static final String BOOBS_SEARCHING_URL = "http://api.oboobs.ru/boobs/model/";
    private static final int RESPONSE_BOOBS_IMAGES_DEFAULT_COUNT = 1;
    private static final int RESPONSE_BOOBS_IMAGES_MAX_COUNT = 10;

    private final Bot bot;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    public List<BotResponse> parse(BotRequest request) throws BotException {
        Message message = request.getMessage();
        bot.sendUploadPhoto(message.getChatId());

        List<BoobsData> boobsDataList;
        String commandArgument = message.getCommandArgument();
        if (commandArgument != null) {
            if (TextUtils.isThatPositiveInteger(commandArgument)) {
                int boobsCount;
                try {
                    boobsCount = Integer.parseInt(commandArgument);
                } catch (NumberFormatException e) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                if (boobsCount < 1) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                boobsDataList = getRandomBoobs(boobsCount);
            } else {
                boobsDataList = searchingForBoobs(commandArgument);
            }
        } else {
            boobsDataList = getRandomBoobs(RESPONSE_BOOBS_IMAGES_DEFAULT_COUNT);
        }

        return returnResponse(new FileResponse(message)
                .setText("18+")
                .addFiles(boobsDataList.stream().map(this::mapToFile).toList()));
    }

    private File mapToFile(BoobsData boobsData) {
        String model = boobsData.getModel();
        if (model == null) {
            model = "";
        }
        String imageName = boobsData.getPreview().substring(boobsData.getPreview().indexOf("boobs_preview/") + 14);

        return new File(FileType.IMAGE, BOOBS_IMAGE_URL + imageName, model, new FileSettings().setSpoiler(true));
    }

    private List<BoobsData> getRandomBoobs(int count) {
        if (count > RESPONSE_BOOBS_IMAGES_MAX_COUNT) {
            count = RESPONSE_BOOBS_IMAGES_MAX_COUNT;
        }

        BoobsData[] boobsData = getBoobsData(String.format(RANDOM_BOOBS_API_TEMPLATE, count));
        if (boobsData.length == 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return Arrays.asList(boobsData);
    }

    private List<BoobsData> searchingForBoobs(String model) {
        BoobsData[] boobsData = getBoobsData(BOOBS_SEARCHING_URL + model);
        if (boobsData.length == 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }

        return Arrays.stream(boobsData).limit(RESPONSE_BOOBS_IMAGES_MAX_COUNT).toList();
    }

    private BoobsData[] getBoobsData(String url) {
        ResponseEntity<BoobsData[]> response;
        try {
            response = botRestTemplate.getForEntity(url, BoobsData[].class);
        } catch (RestClientException e) {
            log.error("Error receiving boobs");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        BoobsData[] boobsData = response.getBody();
        if (boobsData == null) {
            log.debug("Empty response from service");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return boobsData;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BoobsData implements Serializable {
        private String model;
        private String preview;
        private String id;
        private Long rank;
        private String author;
    }
}
