package org.telegram.bot.providers.sber.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.enums.SberScope;
import org.telegram.bot.exception.GettingSberAccessTokenException;
import org.telegram.bot.exception.speech.SpeechParseException;
import org.telegram.bot.providers.sber.SberTokenProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaluteSpeechParserTest {

    private static final byte[] FILE = "123".getBytes();

    @Mock
    private SberTokenProvider sberTokenProvider;
    @Mock
    private RestTemplate sberRestTemplate;

    @Mock
    private ResponseEntity<SaluteSpeechParser.SpeechRecognizeResult> speechRecognizeResultResponseEntity;

    @InjectMocks
    private SaluteSpeechParser saluteSpeechParser;

    @Test
    void parseWithNullDurationTest() {
        assertThrows(SpeechParseException.class, () -> saluteSpeechParser.parse(FILE, null));
    }

    @Test
    void parseWithTooLongDurationTest() {
        assertThrows(SpeechParseException.class, () -> saluteSpeechParser.parse(FILE, 999));
    }

    @Test
    void recognizeWithGettingSberAccessTokenExceptionTest() throws GettingSberAccessTokenException {
        when(sberTokenProvider.getToken(SberScope.SALUTE_SPEECH_PERS)).thenThrow(new GettingSberAccessTokenException("error"));
        assertThrows(SpeechParseException.class, () -> saluteSpeechParser.parse(FILE, 30));
    }

    @Test
    void recognizeWithRestClientExceptionTest() throws GettingSberAccessTokenException {
        when(sberTokenProvider.getToken(SberScope.SALUTE_SPEECH_PERS)).thenReturn("accessToken");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<SaluteSpeechParser.SpeechRecognizeResult>>any()))
                .thenThrow(new RestClientException("error"));

        assertThrows(SpeechParseException.class, () -> saluteSpeechParser.parse(FILE, 30));
    }

    @Test
    void recognizeWithEmptyResponseTest() throws GettingSberAccessTokenException {
        when(sberTokenProvider.getToken(SberScope.SALUTE_SPEECH_PERS)).thenReturn("accessToken");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<SaluteSpeechParser.SpeechRecognizeResult>>any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        assertThrows(SpeechParseException.class, () -> saluteSpeechParser.parse(FILE, 30));
    }

    @Test
    void recognizeTest() throws SpeechParseException, GettingSberAccessTokenException {
        List<String> result = List.of("test1", "test2");
        final String expectedResult = String.join(". ", result);
        SaluteSpeechParser.SpeechRecognizeResult speechRecognizeResult = new SaluteSpeechParser.SpeechRecognizeResult().setStatus(200)
                        .setResult(result);

        when(speechRecognizeResultResponseEntity.getBody()).thenReturn(speechRecognizeResult);
        when(sberTokenProvider.getToken(SberScope.SALUTE_SPEECH_PERS)).thenReturn("accessToken");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<SaluteSpeechParser.SpeechRecognizeResult>>any()))
                .thenReturn(speechRecognizeResultResponseEntity);

        String actualResult = saluteSpeechParser.parse(FILE, 30);

        assertEquals(expectedResult, actualResult);
    }

}