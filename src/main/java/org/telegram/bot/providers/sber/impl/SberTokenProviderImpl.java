package org.telegram.bot.providers.sber.impl;

import lombok.RequiredArgsConstructor;
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
import org.telegram.bot.dto.SberAccessTokenResponseDto;
import org.telegram.bot.enums.SberScope;
import org.telegram.bot.exception.GettingSberAccessTokenException;
import org.telegram.bot.providers.sber.SberTokenProvider;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Service
@Slf4j
public class SberTokenProviderImpl implements SberTokenProvider {

    private final PropertiesConfig propertiesConfig;
    private final RestTemplate insecureRestTemplate;

    private final Map<SberScope, SberAccessTokenResponseDto> accessTokenMap = new ConcurrentHashMap<>();

    private static final String GET_ACCESS_TOKEN_API_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";

    @PostConstruct
    private void postConstruct() {
        long now = Instant.now().toEpochMilli();
        Arrays.stream(SberScope.values()).forEach(sberScope ->
                accessTokenMap.put(sberScope, new SberAccessTokenResponseDto().setAccessToken(null).setExpiresAt(now)));
    }

    @Override
    public String getToken(SberScope sberScope) throws GettingSberAccessTokenException {
        SberAccessTokenResponseDto accessToken = accessTokenMap.get(sberScope);

        if (isTokenExpired(accessToken)) {
            accessToken = getFromApi(sberScope);
        }

        return Optional.of(accessToken)
                .map(SberAccessTokenResponseDto::getAccessToken)
                .orElseThrow(() -> new GettingSberAccessTokenException("empty sber token"));
    }

    private boolean isTokenExpired(SberAccessTokenResponseDto accessToken) {
        return Instant.now().toEpochMilli() > accessToken.getExpiresAt();
    }

    private SberAccessTokenResponseDto getFromApi(SberScope sberScope) throws GettingSberAccessTokenException {
        String secret = getSecret(sberScope);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(secret);
        headers.set("RqUID", UUID.randomUUID().toString());

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("scope", sberScope.name());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<SberAccessTokenResponseDto> response;
        try {
            response = insecureRestTemplate.postForEntity(GET_ACCESS_TOKEN_API_URL, request, SberAccessTokenResponseDto.class);
        } catch (RestClientException e) {
            log.error("Failed to get sber access token", e);
            throw new GettingSberAccessTokenException(e.getMessage());
        }

        SberAccessTokenResponseDto accessToken = response.getBody();
        if (accessToken == null) {
            log.error("Failed to get salute speech access token: empty body");
            throw new GettingSberAccessTokenException("Failed to get sber access token: empty body");
        }

        return accessToken;
    }

    private String getSecret(SberScope sberScope) throws GettingSberAccessTokenException {
        String secret = null;

        switch (sberScope) {
            case SALUTE_SPEECH_PERS:
                secret = propertiesConfig.getSaluteSpeechSecret();
                break;
            case GIGACHAT_API_PERS:
                secret = propertiesConfig.getGigaChatSecret();
                break;
        }

        if (secret == null) {
            throw new GettingSberAccessTokenException("Unable to find " + sberScope + " token");
        }

        return secret;
    }

}
