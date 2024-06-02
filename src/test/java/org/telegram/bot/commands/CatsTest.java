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
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.telegram.bot.TestUtils.*;

@ExtendWith(MockitoExtension.class)
class CatsTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private ResponseEntity<Object> response;

    @InjectMocks
    private Cats cats;

    @Test
    void parseWithArgumentsTest() {
        BotRequest request = getRequestFromGroup("cats test");
        List<BotResponse> botResponses = cats.parse(request);
        verify(bot, never()).sendUploadPhoto(request.getMessage().getChatId());
        assertTrue(botResponses.isEmpty());
    }

    @Test
    void parseWithNoResponseTest() {
        BotRequest request = getRequestFromGroup("cats");
        when(botRestTemplate.getForEntity(anyString(), any())).thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> cats.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithEmptyResponseTest() {
        BotRequest request = getRequestFromGroup("cats");
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);

        assertThrows(BotException.class, () -> cats.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithGifResponseTest() {
        BotRequest request = getRequestFromGroup("cats");
        Cats.Cat cat = new Cats.Cat();
        cat.setUrl("url.gif");
        Cats.Cat[] catsArray = {cat};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(catsArray);

        BotResponse response = cats.parse(request).get(0);
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        checkDefaultFileResponseParams(response);
    }

    @Test
    void parseWithPhotoResponseTest() {
        BotRequest request = getRequestFromGroup("cats");
        Cats.Cat cat = new Cats.Cat();
        cat.setUrl("url");
        Cats.Cat[] catsArray = {cat};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(catsArray);

        BotResponse response = cats.parse(request).get(0);
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        checkDefaultFileResponseImageParams(response);
    }

}