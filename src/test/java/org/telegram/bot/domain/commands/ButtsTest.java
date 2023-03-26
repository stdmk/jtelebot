package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ButtsTest {
    @Mock
    SpeechService speechService;
    @Mock
    RestTemplate botRestTemplate;
    @Mock
    ResponseEntity<Object> response;

    @InjectMocks
    Butts butts;

    @Test
    void parseWithNoResponseTest() {
        when(botRestTemplate.getForEntity(anyString(), any())).thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> butts.parse(TestUtils.getUpdate()));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithNullButtsTest() {
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);

        assertThrows(BotException.class, () -> butts.parse(TestUtils.getUpdate()));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseTest() {
        Butts.ButtsCount buttsCount = new Butts.ButtsCount();
        buttsCount.setCount(1);
        Butts.ButtsCount[] buttsCountArray = {buttsCount};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(buttsCountArray);

        assertDoesNotThrow(() -> butts.parse(TestUtils.getUpdate()));
    }
}