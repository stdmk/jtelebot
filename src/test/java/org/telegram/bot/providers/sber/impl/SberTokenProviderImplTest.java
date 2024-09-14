package org.telegram.bot.providers.sber.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SberTokenProviderImplTest {

    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private RestTemplate sberRestTemplate;

    @Mock
    private ResponseEntity<SberAccessTokenResponseDto> sberAccessTokenResponseDtoResponseEntity;

    @InjectMocks
    private SberTokenProviderImpl sberTokenProvider;

    @BeforeEach
    void init() {
        when(propertiesConfig.getGigaChatSecret()).thenReturn("gigachat");
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn("salute");
        ReflectionTestUtils.invokeMethod(sberTokenProvider, "postConstruct");
    }

    @Test
    void updateAccessTokenWithoutSecretTest() {
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn(null);
        assertThrows(GettingSberAccessTokenException.class, () -> sberTokenProvider.getToken(SberScope.SALUTE_SPEECH_PERS));
    }

    @Test
    void updateAccessTokenWithRestClientExceptionTest() {
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn("token");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenThrow(new RestClientException("error"));
        assertThrows(GettingSberAccessTokenException.class, () -> sberTokenProvider.getToken(SberScope.SALUTE_SPEECH_PERS));
    }

    @Test
    void updateAccessTokenWithEmptyResponseTest() {
        when(propertiesConfig.getGigaChatSecret()).thenReturn("token");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
        assertThrows(GettingSberAccessTokenException.class, () -> sberTokenProvider.getToken(SberScope.GIGACHAT_API_PERS));
    }

    @Test
    void updateAccessTest() {
        SberAccessTokenResponseDto response = new SberAccessTokenResponseDto()
                .setAccessToken("access_token")
                .setExpiresAt(Instant.now().plusSeconds(360).toEpochMilli());
        when(sberAccessTokenResponseDtoResponseEntity.getBody()).thenReturn(response);
        when(propertiesConfig.getGigaChatSecret()).thenReturn("token");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<SberAccessTokenResponseDto>>any()))
                .thenReturn(sberAccessTokenResponseDtoResponseEntity);

        assertDoesNotThrow(() -> sberTokenProvider.getToken(SberScope.GIGACHAT_API_PERS));
    }

    @Test
    void updateTokensWithGettingSberAccessTokenExceptionTest() {
        when(propertiesConfig.getGigaChatSecret()).thenReturn("token");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        assertDoesNotThrow(() -> sberTokenProvider.updateTokens());

        Map<SberScope, SberAccessTokenResponseDto> accessTokenMap = (Map<SberScope, SberAccessTokenResponseDto>) ReflectionTestUtils.getField(sberTokenProvider, "accessTokenMap");
        assertNotNull(accessTokenMap);
        accessTokenMap.values().stream().map(SberAccessTokenResponseDto::getAccessToken).forEach(Assertions::assertNull);
    }

    @Test
    void updateTokensTest() {
        SberScope sperScope = SberScope.GIGACHAT_API_PERS;
        Instant now = Instant.now();

        SberAccessTokenResponseDto response = new SberAccessTokenResponseDto()
                .setAccessToken("access_token")
                .setExpiresAt(now.plusSeconds(360).toEpochMilli());
        when(sberAccessTokenResponseDtoResponseEntity.getBody()).thenReturn(response);
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn("secret");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<SberAccessTokenResponseDto>>any()))
                .thenReturn(sberAccessTokenResponseDtoResponseEntity);

        Map<SberScope, SberAccessTokenResponseDto> accessTokenMap = (Map<SberScope, SberAccessTokenResponseDto>) ReflectionTestUtils.getField(sberTokenProvider, "accessTokenMap");
        assertNotNull(accessTokenMap);
        SberAccessTokenResponseDto accessToken = accessTokenMap.get(sperScope);
        accessToken.setExpiresAt(now.plusSeconds(720).toEpochMilli());
        accessToken.setAccessToken("token2");
        accessTokenMap.put(sperScope, accessToken);

        sberTokenProvider.updateTokens();

        accessTokenMap.values().stream().map(SberAccessTokenResponseDto::getAccessToken).forEach(Assertions::assertNotNull);

    }

}