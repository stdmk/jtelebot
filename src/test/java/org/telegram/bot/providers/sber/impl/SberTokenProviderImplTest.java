package org.telegram.bot.providers.sber.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.dto.SberAccessTokenResponseDto;
import org.telegram.bot.enums.SberScope;
import org.telegram.bot.exception.GettingSberAccessTokenException;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SberTokenProviderImplTest {

    private static final Instant NOW = Instant.parse("2000-01-01T00:00:00Z");

    @Mock
    private Clock clock;
    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private RestTemplate sberRestTemplate;

    @Mock
    private ResponseEntity<SberAccessTokenResponseDto> sberAccessTokenResponseDtoResponseEntity;

    @InjectMocks
    private SberTokenProviderImpl sberTokenProvider;

    @Test
    void updateAccessTokenWithoutSecretTest() {
        when(clock.instant()).thenReturn(NOW);
        when(propertiesConfig.getGigaChatSecret()).thenReturn("gigachat");
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn(null);
        assertThrows(GettingSberAccessTokenException.class, () -> sberTokenProvider.getToken(SberScope.SALUTE_SPEECH_PERS));
    }

    @Test
    void updateAccessTokenWithRestClientExceptionTest() {
        when(clock.instant()).thenReturn(NOW);
        when(propertiesConfig.getGigaChatSecret()).thenReturn("gigachat");
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn("token");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenThrow(new RestClientException("error"));
        assertThrows(GettingSberAccessTokenException.class, () -> sberTokenProvider.getToken(SberScope.SALUTE_SPEECH_PERS));
    }

    @Test
    void updateAccessTokenWithEmptyResponseTest() {
        when(clock.instant()).thenReturn(NOW);
        when(propertiesConfig.getGigaChatSecret()).thenReturn("gigachat");
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn("token");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
        assertThrows(GettingSberAccessTokenException.class, () -> sberTokenProvider.getToken(SberScope.GIGACHAT_API_PERS));
    }

    @Test
    void updateAccessTest() {
        when(clock.instant()).thenReturn(NOW);
        SberAccessTokenResponseDto response = new SberAccessTokenResponseDto()
                .setAccessToken("access_token")
                .setExpiresAt(Instant.now().plusSeconds(360).toEpochMilli());
        when(sberAccessTokenResponseDtoResponseEntity.getBody()).thenReturn(response);
        when(propertiesConfig.getGigaChatSecret()).thenReturn("gigachat");
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn("token");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<SberAccessTokenResponseDto>>any()))
                .thenReturn(sberAccessTokenResponseDtoResponseEntity);

        assertDoesNotThrow(() -> sberTokenProvider.getToken(SberScope.GIGACHAT_API_PERS));
    }

    @Test
    void updateTokensWithGettingSberAccessTokenExceptionTest() {
        when(clock.instant()).thenReturn(NOW);
        when(propertiesConfig.getGigaChatSecret()).thenReturn("gigachat");
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn("token");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        assertDoesNotThrow(() -> sberTokenProvider.updateTokens());

        Map<SberScope, SberAccessTokenResponseDto> accessTokenMap = (Map<SberScope, SberAccessTokenResponseDto>) ReflectionTestUtils.getField(sberTokenProvider, "accessTokenMap");
        assertNotNull(accessTokenMap);
        accessTokenMap.values().stream().map(SberAccessTokenResponseDto::getAccessToken).forEach(Assertions::assertNull);
    }

    @Test
    void updateTokensTest() {
        when(clock.instant()).thenReturn(NOW);
        SberScope sperScope = SberScope.GIGACHAT_API_PERS;

        SberAccessTokenResponseDto response = new SberAccessTokenResponseDto()
                .setAccessToken("access_token")
                .setExpiresAt(NOW.plusSeconds(360).toEpochMilli());
        when(sberAccessTokenResponseDtoResponseEntity.getBody()).thenReturn(response);
        when(propertiesConfig.getGigaChatSecret()).thenReturn("gigachat");
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn("token");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<SberAccessTokenResponseDto>>any()))
                .thenReturn(sberAccessTokenResponseDtoResponseEntity);

        ReflectionTestUtils.invokeMethod(sberTokenProvider, "initMap");

        Map<SberScope, SberAccessTokenResponseDto> accessTokenMap = (Map<SberScope, SberAccessTokenResponseDto>) ReflectionTestUtils.getField(sberTokenProvider, "accessTokenMap");
        assertNotNull(accessTokenMap);
        SberAccessTokenResponseDto accessToken = accessTokenMap.get(sperScope);
        accessToken.setExpiresAt(NOW.plusSeconds(720).toEpochMilli());
        accessToken.setAccessToken("token2");
        accessTokenMap.put(sperScope, accessToken);

        sberTokenProvider.updateTokens();

        accessTokenMap.values().stream().map(SberAccessTokenResponseDto::getAccessToken).forEach(Assertions::assertNotNull);

    }

}