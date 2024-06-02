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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ButtsTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private ResponseEntity<Object> response;

    @InjectMocks
    private Butts butts;

    @Test
    void parseWithNoResponseTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        when(botRestTemplate.getForEntity(anyString(), any())).thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> butts.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithNullButtsTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);

        assertThrows(BotException.class, () -> butts.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        Butts.ButtsCount buttsCount = new Butts.ButtsCount();
        buttsCount.setCount(1);
        Butts.ButtsCount[] buttsCountArray = {buttsCount};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(buttsCountArray);

        BotResponse botResponse = butts.parse(request).get(0);
        FileResponse image = TestUtils.checkDefaultFileResponseImageParams(botResponse);

        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        TestUtils.checkDefaultFileResponseImageParams(image, true);
    }
}