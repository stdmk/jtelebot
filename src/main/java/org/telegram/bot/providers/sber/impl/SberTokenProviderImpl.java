package org.telegram.bot.providers.sber.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.dto.SberAccessTokenResponseDto;
import org.telegram.bot.enums.SberScope;
import org.telegram.bot.exception.GettingSberAccessTokenException;
import org.telegram.bot.providers.sber.SberTokenProvider;

import java.time.Clock;
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

    private final Clock clock;
    private final PropertiesConfig propertiesConfig;
    private final RestTemplate sberRestTemplate;

    private final Map<SberScope, SberAccessTokenResponseDto> accessTokenMap = new ConcurrentHashMap<>();

    private static final String GET_ACCESS_TOKEN_API_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";

    @Override
    public String getToken(SberScope sberScope) throws GettingSberAccessTokenException {
        if (accessTokenMap.isEmpty()) {
            initMap();
        }

        SberAccessTokenResponseDto accessToken = accessTokenMap.get(sberScope);
        if (accessToken == null) {
            throw new GettingSberAccessTokenException("empty sber token");
        }

        if (isTokenExpired(accessToken)) {
            accessToken = getFromApi(sberScope);
            accessTokenMap.put(sberScope, accessToken);
        }

        return Optional.of(accessToken)
                .map(SberAccessTokenResponseDto::getAccessToken)
                .orElseThrow(() -> new GettingSberAccessTokenException("empty sber token"));
    }

    @Override
    public void updateTokens() {
        if (accessTokenMap.isEmpty()) {
            initMap();
        }

        accessTokenMap.forEach((sberScope, accessToken) -> {
            if (isTokenExpired(accessToken)) {
                try {
                    accessToken = getFromApi(sberScope);
                } catch (Exception e) {
                    log.error("Failed to update token for scope {}", sberScope, e);
                }
                accessTokenMap.put(sberScope, accessToken);
            }
        });
    }

    private void initMap() {
        long now = Instant.now(clock).toEpochMilli();
        Arrays.stream(SberScope.values())
                .filter(sberScope -> StringUtils.isNotEmpty(sberScope.getSecretFunction.apply(propertiesConfig)))
                .forEach(sberScope ->
                        accessTokenMap.put(sberScope, new SberAccessTokenResponseDto().setAccessToken(null).setExpiresAt(now)));
    }

    private boolean isTokenExpired(SberAccessTokenResponseDto accessToken) {
        return accessToken.getAccessToken() == null || Instant.now(clock).toEpochMilli() > accessToken.getExpiresAt();
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
            response = sberRestTemplate.postForEntity(GET_ACCESS_TOKEN_API_URL, request, SberAccessTokenResponseDto.class);
        } catch (Exception e) {
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
        String secret = sberScope.getSecretFunction.apply(propertiesConfig);
        
        if (secret == null) {
            throw new GettingSberAccessTokenException("Unable to find " + sberScope + " token");
        }

        return secret;
    }

}
