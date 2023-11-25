package org.telegram.bot.providers.sber.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.enums.SberScope;
import org.telegram.bot.exception.GettingSberAccessTokenException;
import org.telegram.bot.exception.SpeechSynthesizeException;
import org.telegram.bot.providers.sber.SberApiProvider;
import org.telegram.bot.providers.sber.SberTokenProvider;
import org.telegram.bot.providers.sber.SpeechSynthesizer;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Service
@Slf4j
public class SaluteSpeechSynthesizer implements SpeechSynthesizer, SberApiProvider {

    private static final Integer TEXT_LENGTH_LIMIT = 4000;
    private static final String DEFAULT_VOICE = "Bys_24000";
    private static final String DEFAULT_VOICE_FORMAT = "opus";
    private static final String SALUTE_SPEECH_API_URL = "https://smartspeech.sber.ru/rest/v1/text:synthesize?" +
            "format=" + DEFAULT_VOICE_FORMAT + "&voice=" + DEFAULT_VOICE;

    private final SberTokenProvider sberTokenProvider;
    private final RestTemplate insecureRestTemplate;

    @Override
    public byte[] synthesize(String text) throws SpeechSynthesizeException {
        checkTextLength(text);

        String accessToken;
        try {
            accessToken = sberTokenProvider.getToken(getScope());
        } catch (GettingSberAccessTokenException e) {
            log.error("Error getting salute speech api token", e);
            throw new SpeechSynthesizeException(e.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Content-Type", "application/text");

        HttpEntity<byte[]> request = new HttpEntity<>(text.getBytes(StandardCharsets.UTF_8), headers);

        ResponseEntity<byte[]> response;
        try {
            response = insecureRestTemplate.postForEntity(SALUTE_SPEECH_API_URL, request, byte[].class);
        } catch (RestClientException e) {
            log.error("Failed to get response from SaluteSpeech", e);
            throw new SpeechSynthesizeException(e.getMessage());
        }

        byte[] voice = response.getBody();
        if (voice == null) {
            log.error("Failed to get response from SaluteSpeech: empty body");
            throw new SpeechSynthesizeException("Failed to get response from SaluteSpeech: empty body");
        }

        return voice;
    }

    private void checkTextLength(String text) throws SpeechSynthesizeException {
        if (text == null || text.length() > TEXT_LENGTH_LIMIT) {
            throw new SpeechSynthesizeException("Too long text");
        }
    }

    @Override
    public SberScope getScope() {
        return SberScope.SALUTE_SPEECH_PERS;
    }
}
