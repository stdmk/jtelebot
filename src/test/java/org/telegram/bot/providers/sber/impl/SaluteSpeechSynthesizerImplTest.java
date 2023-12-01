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
import org.telegram.bot.enums.SaluteSpeechVoice;
import org.telegram.bot.enums.SberScope;
import org.telegram.bot.exception.GettingSberAccessTokenException;
import org.telegram.bot.exception.SpeechSynthesizeException;
import org.telegram.bot.providers.sber.SberTokenProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaluteSpeechSynthesizerImplTest {

    @Mock
    private SberTokenProvider sberTokenProvider;
    @Mock
    private RestTemplate sberRestTemplate;

    @Mock
    private ResponseEntity<byte[]> responseEntity;

    @InjectMocks
    private SaluteSpeechSynthesizerImpl saluteSpeechSynthesizerImpl;

    @Test
    void synthesizeTooLongTextTest() {
        final String expectedErrorText = "Too long text";
        String text = "1".repeat(4001);

        SpeechSynthesizeException sberAccessTokenException =
                assertThrows((SpeechSynthesizeException.class), () -> saluteSpeechSynthesizerImpl.synthesize(text, "en"));
        assertEquals(expectedErrorText, sberAccessTokenException.getMessage());
    }

    @Test
    void synthesizeWithoutTextTest() {
        final String expectedErrorText = "Empty text";
        String text = "";

        SpeechSynthesizeException sberAccessTokenException =
                assertThrows((SpeechSynthesizeException.class), () -> saluteSpeechSynthesizerImpl.synthesize(text, "en"));
        assertEquals(expectedErrorText, sberAccessTokenException.getMessage());
    }

    @Test
    void synthesizeWithGettingSberAccessTokenExceptionTest() throws GettingSberAccessTokenException {
        final String expectedErrorText = "error";
        String text = "text";

        when(sberTokenProvider.getToken(SberScope.SALUTE_SPEECH_PERS)).thenThrow(new GettingSberAccessTokenException(expectedErrorText));

        SpeechSynthesizeException speechSynthesizeException =
                assertThrows((SpeechSynthesizeException.class), () -> saluteSpeechSynthesizerImpl.synthesize(text, "en"));
        assertEquals(expectedErrorText, speechSynthesizeException.getMessage());
    }

    @Test
    void synthesizeWithRestClientExceptionTest() throws GettingSberAccessTokenException {
        final String expectedErrorText = "error";
        String text = "text";

        when(sberTokenProvider.getToken(SberScope.SALUTE_SPEECH_PERS)).thenReturn("token");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenThrow(new RestClientException(expectedErrorText));

        SpeechSynthesizeException speechSynthesizeException =
                assertThrows((SpeechSynthesizeException.class), () -> saluteSpeechSynthesizerImpl.synthesize(text, "en"));
        assertEquals(expectedErrorText, speechSynthesizeException.getMessage());
    }

    @Test
    void synthesizeWithEmptyResponseTest() throws GettingSberAccessTokenException {
        String text = "text";

        when(sberTokenProvider.getToken(SberScope.SALUTE_SPEECH_PERS)).thenReturn("token");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        assertThrows((SpeechSynthesizeException.class), () -> saluteSpeechSynthesizerImpl.synthesize(text, "en"));
    }

    @Test
    void synthesizeTest() throws GettingSberAccessTokenException, SpeechSynthesizeException {
        String text = "text";
        byte[] voice = text.getBytes();

        when(responseEntity.getBody()).thenReturn(voice);
        when(sberTokenProvider.getToken(SberScope.SALUTE_SPEECH_PERS)).thenReturn("token");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<byte[]>>any()))
                .thenReturn(responseEntity);

        byte[] bytes = saluteSpeechSynthesizerImpl.synthesize(text, "en");

        assertEquals(voice, bytes);
    }

    @Test
    void synthesizeUnknownLangTest() throws GettingSberAccessTokenException, SpeechSynthesizeException {
        String text = "text";
        byte[] voice = text.getBytes();

        when(responseEntity.getBody()).thenReturn(voice);
        when(sberTokenProvider.getToken(SberScope.SALUTE_SPEECH_PERS)).thenReturn("token");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<byte[]>>any()))
                .thenReturn(responseEntity);

        byte[] bytes = saluteSpeechSynthesizerImpl.synthesize(text, "aa");

        assertEquals(voice, bytes);
    }

    @Test
    void synthesizeWithSaluteSpeechVoiceWithGettingSberAccessTokenExceptionTest() throws GettingSberAccessTokenException {
        final String expectedErrorText = "error";
        String text = "text";

        when(sberTokenProvider.getToken(SberScope.SALUTE_SPEECH_PERS)).thenThrow(new GettingSberAccessTokenException(expectedErrorText));

        SpeechSynthesizeException speechSynthesizeException =
                assertThrows((SpeechSynthesizeException.class), () -> saluteSpeechSynthesizerImpl.synthesize(text, "en", SaluteSpeechVoice.KIN));
        assertEquals(expectedErrorText, speechSynthesizeException.getMessage());
    }

    @Test
    void synthesizeWithSaluteSpeechVoiceTest() throws GettingSberAccessTokenException, SpeechSynthesizeException {
        String text = "text";
        byte[] voice = text.getBytes();

        when(responseEntity.getBody()).thenReturn(voice);
        when(sberTokenProvider.getToken(SberScope.SALUTE_SPEECH_PERS)).thenReturn("token");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<byte[]>>any()))
                .thenReturn(responseEntity);

        byte[] bytes = saluteSpeechSynthesizerImpl.synthesize(text, "en", SaluteSpeechVoice.KIN);

        assertEquals(voice, bytes);
    }

}