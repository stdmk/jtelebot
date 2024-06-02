package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoobsTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private ResponseEntity<Object> response;

    @InjectMocks
    private Boobs boobs;

    @Test
    void parseWithNoResponseTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        when(botRestTemplate.getForEntity(anyString(), any())).thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> boobs.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithNullBoobsTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);

        assertThrows(BotException.class, () -> boobs.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        Boobs.BoobsCount boobsCount = new Boobs.BoobsCount();
        boobsCount.setCount(1);
        Boobs.BoobsCount[] boobsCountArray = {boobsCount};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(boobsCountArray);

        BotResponse botResponse = boobs.parse(request).get(0);
        FileResponse image = TestUtils.checkDefaultFileResponseImageParams(botResponse);

        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        TestUtils.checkDefaultFileResponseImageParams(image, true);
    }

}