package org.telegram.bot.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
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
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PhoneNumber implements Command {

    private static final String API_URL = "http://rosreestr.subnets.ru/?format=json&get=num&num=";

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());

        String commandArgument = commandWaitingService.getText(message);

        String responseText;
        if (commandArgument == null) {
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.phonenumber.commandwaitingstart}";
        } else {
            responseText = getPhoneInfo(commandArgument);
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML));
    }

    private String getPhoneInfo(String number) {
        ApiResponse apiResponse = getApiAnswer(number);

        if (apiResponse.hasError()) {
            return apiResponse.getError();
        } else {
            PhoneInfo phoneInfo = apiResponse.getPhoneInfo();
            return "<b>${command.phonenumber.operator}:</b> " + phoneInfo.getOperator() + "\n" +
                    "<b>${command.phonenumber.region}:</b> " + phoneInfo.getRegion();
        }
    }

    private ApiResponse getApiAnswer(String number) {
        ResponseEntity<ApiResponse> response;
        try {
            response = botRestTemplate.getForEntity(API_URL + number, ApiResponse.class);
        } catch (RestClientException e) { //404 Not Found: [{"error":"Нет данных"}]
            String errorText = findErrorTextInException(e);
            if (errorText != null) {
                return new ApiResponse().setError(errorText);
            }

            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return Optional.of(response)
                .map(HttpEntity::getBody)
                .orElseThrow(() -> new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE)));
    }

    @Nullable
    private String findErrorTextInException(Exception e) {
        String errorMessage = e.getMessage();
        if (errorMessage != null) {
            int errorFieldIndex = errorMessage.indexOf("\"error\":\"");
            if (errorFieldIndex >= 0) {
                String substring = errorMessage.substring(errorFieldIndex + 9);
                int stopValueIndex = substring.indexOf("\"");
                if (stopValueIndex > 0) {
                    return substring.substring(0, stopValueIndex);
                }
            }
        }

        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @Accessors(chain = true)
    public static class ApiResponse {
        @JsonProperty("0")
        private PhoneInfo phoneInfo;

        @JsonProperty("error")
        private String error;

        public boolean hasError() {
            return this.error != null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @Accessors(chain = true)
    public static class PhoneInfo {
        private String operator;
        private String region;
    }

}
