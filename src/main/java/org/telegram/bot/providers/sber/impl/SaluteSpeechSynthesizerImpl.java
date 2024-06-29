package org.telegram.bot.providers.sber.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.enums.SaluteSpeechVoice;
import org.telegram.bot.enums.SberScope;
import org.telegram.bot.exception.GettingSberAccessTokenException;
import org.telegram.bot.exception.speech.SpeechSynthesizeException;
import org.telegram.bot.exception.speech.SpeechSynthesizeNoApiResponseException;
import org.telegram.bot.providers.sber.SaluteSpeechSynthesizer;
import org.telegram.bot.providers.sber.SberApiProvider;
import org.telegram.bot.providers.sber.SberTokenProvider;
import org.telegram.bot.providers.sber.SpeechSynthesizer;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Service
@Slf4j
public class SaluteSpeechSynthesizerImpl implements SpeechSynthesizer, SaluteSpeechSynthesizer, SberApiProvider {

    private static final Integer TEXT_LENGTH_LIMIT = 4000;
    private static final SaluteSpeechVoice DEFAULT_VOICE = SaluteSpeechVoice.BYS;
    private static final String SALUTE_SPEECH_API_URL_TEMPLATE = "https://smartspeech.sber.ru/rest/v1/text:synthesize?" +
            "format=opus&voice=%s_24000";

    private final SberTokenProvider sberTokenProvider;
    private final RestTemplate sberRestTemplate;

    @Override
    public byte[] synthesize(String text, String langCode) throws SpeechSynthesizeException {
        checkTextLength(text);

        String accessToken;
        try {
            accessToken = sberTokenProvider.getToken(getScope());
        } catch (GettingSberAccessTokenException e) {
            log.error("Error getting salute speech api token", e);
            throw new SpeechSynthesizeNoApiResponseException(e.getMessage());
        }

        SaluteSpeechVoice saluteSpeechVoice = SaluteSpeechVoice.getByLangCode(langCode);
        if (saluteSpeechVoice == null) {
            saluteSpeechVoice = DEFAULT_VOICE;
        }

        return getFromApi(accessToken, saluteSpeechVoice, text);
    }

    @Override
    public byte[] synthesize(String text, String langCode, SaluteSpeechVoice saluteSpeechVoice) throws SpeechSynthesizeException {
        checkTextLength(text);

        String accessToken;
        try {
            accessToken = sberTokenProvider.getToken(getScope());
        } catch (GettingSberAccessTokenException e) {
            log.error("Error getting salute speech api token", e);
            throw new SpeechSynthesizeException(e.getMessage());
        }

        return getFromApi(accessToken, saluteSpeechVoice, text);
    }

    private void checkTextLength(String text) throws SpeechSynthesizeException {
        if (text == null || text.isEmpty()) {
            throw new SpeechSynthesizeException("Empty text");
        } else if (text.length() > TEXT_LENGTH_LIMIT) {
            throw new SpeechSynthesizeException("Too long text");
        }
    }

    private byte[] getFromApi(String accessToken, SaluteSpeechVoice saluteSpeechVoice, String text) throws SpeechSynthesizeException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Content-Type", "application/text");

        HttpEntity<byte[]> request = new HttpEntity<>(text.getBytes(StandardCharsets.UTF_8), headers);

        String url = String.format(SALUTE_SPEECH_API_URL_TEMPLATE, saluteSpeechVoice.getCode());
        ResponseEntity<byte[]> response;
        try {
            response = sberRestTemplate.postForEntity(url, request, byte[].class);
        } catch (RestClientException e) {
            log.error("Failed to get response from SaluteSpeech", e);
            throw new SpeechSynthesizeNoApiResponseException(e.getMessage());
        }

        byte[] voice = response.getBody();
        if (voice == null) {
            log.error("Failed to get response from SaluteSpeech: empty body");
            throw new SpeechSynthesizeNoApiResponseException("Failed to get response from SaluteSpeech: empty body");
        }

        return voice;
    }

    @Override
    public SberScope getScope() {
        return SberScope.SALUTE_SPEECH_PERS;
    }

}
