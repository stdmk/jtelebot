package org.telegram.bot.providers.sber.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.enums.SberScope;
import org.telegram.bot.exception.GettingSberAccessTokenException;
import org.telegram.bot.exception.speech.SpeechParseException;
import org.telegram.bot.exception.speech.TooLongSpeechException;
import org.telegram.bot.providers.sber.SberApiProvider;
import org.telegram.bot.providers.sber.SberTokenProvider;
import org.telegram.bot.providers.sber.SpeechParser;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class SaluteSpeechParser implements SberApiProvider, SpeechParser {

    private static final Integer SPEECH_DURATION_LIMIT = 60;
    private static final String SALUTE_SPEECH_API_URL = "https://smartspeech.sber.ru/rest/v1/speech:recognize";
    private static final String TELEGRAM_VOICE_CONTENT_TYPE = "audio/ogg;codecs=opus";

    private final SberTokenProvider sberTokenProvider;
    private final RestTemplate sberRestTemplate;

    @Override
    public SberScope getScope() {
        return SberScope.SALUTE_SPEECH_PERS;
    }

    @Override
    public String parse(byte[] file, Integer duration) throws SpeechParseException {
        checkSpeechDuration(duration);
        return recognize(file);
    }

    private void checkSpeechDuration(Integer duration) throws SpeechParseException {
        if (duration == null || duration > SPEECH_DURATION_LIMIT) {
            throw new TooLongSpeechException("Too long speech");
        }
    }

    private String recognize(byte[] file) throws SpeechParseException {
        String accessToken;
        try {
            accessToken = sberTokenProvider.getToken(getScope());
        } catch (GettingSberAccessTokenException e) {
            log.error("Error getting salute speech api token", e);
            throw new SpeechParseException(e.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Content-Type", TELEGRAM_VOICE_CONTENT_TYPE);

        HttpEntity<byte[]> request = new HttpEntity<>(file, headers);

        ResponseEntity<SpeechRecognizeResult> response;
        try {
            response = sberRestTemplate.postForEntity(SALUTE_SPEECH_API_URL, request, SpeechRecognizeResult.class);
        } catch (RestClientException e) {
            log.error("Failed to get response from SaluteSpeech", e);
            throw new SpeechParseException(e.getMessage());
        }

        SpeechRecognizeResult speechRecognizeResult = response.getBody();
        if (speechRecognizeResult == null) {
            log.error("Failed to get response from SaluteSpeech: empty body");
            throw new SpeechParseException("Failed to get response from SaluteSpeech: empty body");
        }

        return String.join(". ", speechRecognizeResult.getResult());
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpeechRecognizeResult {

        @JsonProperty("result")
        private List<String> result;

        @JsonProperty("status")
        private Integer status;
    }

}
