package org.telegram.bot.providers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.exception.SpeechParseException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaluteSpeechTest {

    private static final String GET_ACCESS_TOKEN_API_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
    private static final String SALUTE_SPEECH_API_URL = "https://smartspeech.sber.ru/rest/v1/speech:recognize";
    private static final byte[] FILE = "123".getBytes();

    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private RestTemplate insecureRestTemplate;

    @Mock
    private ResponseEntity<SaluteSpeech.AccessToken> accessTokenResponseEntity;
    @Mock
    private ResponseEntity<SaluteSpeech.SpeechRecognizeResult> speechRecognizeResultResponseEntity;

    @InjectMocks
    private SaluteSpeech saluteSpeech;

    @Test
    void parseWithNullDurationTest() {
        assertThrows(SpeechParseException.class, () -> saluteSpeech.parse(FILE, null));
    }

    @Test
    void parseWithTooLongDurationTest() {
        assertThrows(SpeechParseException.class, () -> saluteSpeech.parse(FILE, 999));
    }

    @Test
    void updateAccessTokenWithoutSecretTest() {
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn(null);
        assertThrows(SpeechParseException.class, () -> saluteSpeech.parse(FILE, 30));
    }

    @Test
    void updateAccessTokenWithRestClientExceptionTest() {
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn("token");
        when(insecureRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenThrow(new RestClientException("error"));
        assertThrows(SpeechParseException.class, () -> saluteSpeech.parse(FILE, 30));
    }

    @Test
    void updateAccessTokenWithEmptyResponseTest() {
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn("token");
        when(insecureRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
        assertThrows(SpeechParseException.class, () -> saluteSpeech.parse(FILE, 30));
    }

    @Test
    void recognizeWithRestClientExceptionTest() {
        SaluteSpeech.AccessToken accessToken = new SaluteSpeech.AccessToken().setAccessToken("accessToken").setExpiresAt(1700765959L);

        when(accessTokenResponseEntity.getBody()).thenReturn(accessToken);
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn("token");
        when(insecureRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<SaluteSpeech.AccessToken>>any()))
                .thenReturn(accessTokenResponseEntity)
                .thenThrow(new RestClientException("error"));

        assertThrows(SpeechParseException.class, () -> saluteSpeech.parse(FILE, 30));
    }

    @Test
    void recognizeWithEmptyResponseTest() {
        SaluteSpeech.AccessToken accessToken = new SaluteSpeech.AccessToken().setAccessToken("accessToken").setExpiresAt(1700765959L);

        when(accessTokenResponseEntity.getBody()).thenReturn(accessToken);
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn("token");
        when(insecureRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<SaluteSpeech.AccessToken>>any()))
                .thenReturn(accessTokenResponseEntity)
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        assertThrows(SpeechParseException.class, () -> saluteSpeech.parse(FILE, 30));
    }

    @Test
    void recognizeTest() throws SpeechParseException {
        List<String> result = List.of("test1", "test2");
        final String expectedResult = String.join(". ", result);
        SaluteSpeech.AccessToken accessToken = new SaluteSpeech.AccessToken().setAccessToken("accessToken").setExpiresAt(1700765959L);
        SaluteSpeech.SpeechRecognizeResult speechRecognizeResult = new SaluteSpeech.SpeechRecognizeResult().setStatus(200)
                        .setResult(result);

        when(accessTokenResponseEntity.getBody()).thenReturn(accessToken);
        when(speechRecognizeResultResponseEntity.getBody()).thenReturn(speechRecognizeResult);
        when(propertiesConfig.getSaluteSpeechSecret()).thenReturn("token");
        when(insecureRestTemplate.postForEntity(Mockito.eq(GET_ACCESS_TOKEN_API_URL), any(), Mockito.eq(SaluteSpeech.AccessToken.class)))
                .thenReturn(accessTokenResponseEntity);
        when(insecureRestTemplate.postForEntity(Mockito.eq(SALUTE_SPEECH_API_URL), any(), Mockito.eq(SaluteSpeech.SpeechRecognizeResult.class)))
                .thenReturn(speechRecognizeResultResponseEntity);

        String actualResult = saluteSpeech.parse(FILE, 30);

        assertEquals(expectedResult, actualResult);

        String apiAccessToken = (String) ReflectionTestUtils.getField(saluteSpeech, "apiAccessToken");
        LocalDateTime tokenExpiresDateTime = (LocalDateTime) ReflectionTestUtils.getField(saluteSpeech, "tokenExpiresDateTime");
        assertNotNull(apiAccessToken);
        assertNotNull(tokenExpiresDateTime);
    }

}