package org.telegram.bot.providers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.exception.SpeechParseException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class SaluteSpeech implements SpeechParser {

    private static final Integer SPEECH_DURATION_LIMIT = 60;
    private static final String GET_ACCESS_TOKEN_API_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
    private static final String SCOPE = "SALUTE_SPEECH_PERS";
    private static final String SALUTE_SPEECH_API_URL = "https://smartspeech.sber.ru/rest/v1/speech:recognize";
    private static final String TELEGRAM_VOICE_CONTENT_TYPE = "audio/ogg;codecs=opus";

    private final PropertiesConfig propertiesConfig;
    private final RestTemplate insecureRestTemplate;

    private String apiAccessToken;
    private LocalDateTime tokenExpiresDateTime;

    @Override
    public String parse(byte[] file, Integer duration) throws SpeechParseException {
        checkSpeechDuration(duration);
        if (isTokenExpired()) {
            updateAccessToken();
        }

        return recognize(file);
    }

    private void checkSpeechDuration(Integer duration) throws SpeechParseException {
        if (duration == null || duration > SPEECH_DURATION_LIMIT) {
            throw new SpeechParseException("Too long speech");
        }
    }

    private boolean isTokenExpired() {
        return tokenExpiresDateTime == null || LocalDateTime.now().isAfter(tokenExpiresDateTime);
    }

    private void updateAccessToken() throws SpeechParseException {
        String saluteSpeechSecret = propertiesConfig.getSaluteSpeechSecret();
        if (saluteSpeechSecret == null) {
            throw new SpeechParseException("Unable to find SaluteSpeech token");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(saluteSpeechSecret);
        headers.set("RqUID", UUID.randomUUID().toString());

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("scope", SCOPE);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<AccessToken> response;
        try {
            response = insecureRestTemplate.postForEntity(GET_ACCESS_TOKEN_API_URL, request, AccessToken.class);
        } catch (RestClientException e) {
            log.error("Failed to get salute speech access token", e);
            throw new SpeechParseException(e.getMessage());
        }

        AccessToken accessToken = response.getBody();
        if (accessToken == null) {
            log.error("Failed to get salute speech access token: empty body");
            throw new SpeechParseException("Failed to get salute speech access token: empty body");
        }

        apiAccessToken = accessToken.getAccessToken();
        tokenExpiresDateTime = Instant.ofEpochMilli(accessToken.getExpiresAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String recognize(byte[] file) throws SpeechParseException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiAccessToken);
        headers.set("Content-Type", TELEGRAM_VOICE_CONTENT_TYPE);

        HttpEntity<byte[]> request = new HttpEntity<>(file, headers);

        ResponseEntity<SpeechRecognizeResult> response;
        try {
            response = insecureRestTemplate.postForEntity(SALUTE_SPEECH_API_URL, request, SpeechRecognizeResult.class);
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
    public static class AccessToken {

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("expires_at")
        private Long expiresAt;
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
