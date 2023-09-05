package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.telegram.bot.TestUtils.checkDefaultSendPhotoParams;
import static org.telegram.bot.TestUtils.getUpdateFromGroup;

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
        Update update = getUpdateFromGroup();
        when(botRestTemplate.getForEntity(anyString(), any())).thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> boobs.parse(update));
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithNullBoobsTest() {
        Update update = getUpdateFromGroup();
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);

        assertThrows(BotException.class, () -> boobs.parse(update));
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseTest() {
        Update update = getUpdateFromGroup();
        Boobs.BoobsCount boobsCount = new Boobs.BoobsCount();
        boobsCount.setCount(1);
        Boobs.BoobsCount[] boobsCountArray = {boobsCount};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(boobsCountArray);

        SendPhoto sendPhoto = boobs.parse(update);
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        checkDefaultSendPhotoParams(sendPhoto, true);
    }

}