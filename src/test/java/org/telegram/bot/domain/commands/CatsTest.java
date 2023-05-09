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
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatsTest {
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
        PartialBotApiMethod<?> method = cats.parse(TestUtils.getUpdate("cats test"));
        assertNull(method);
    }

    @Test
    void parseWithNoResponseTest() {
        when(botRestTemplate.getForEntity(anyString(), any())).thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> cats.parse(TestUtils.getUpdate("cats")));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithEmptyResponseTest() {
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);

        assertThrows(BotException.class, () -> cats.parse(TestUtils.getUpdate("cats")));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithGifResponseTest() {
        Cats.Cat cat = new Cats.Cat();
        cat.setUrl("url.gif");
        Cats.Cat[] catsArray = {cat};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(catsArray);

        PartialBotApiMethod<?> sendDocument = cats.parse(TestUtils.getUpdate("cats"));
        assertTrue(sendDocument instanceof SendDocument);
    }

    @Test
    void parseWithPhotoResponseTest() {
        Cats.Cat cat = new Cats.Cat();
        cat.setUrl("url");
        Cats.Cat[] catsArray = {cat};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(catsArray);

        PartialBotApiMethod<?> sendDocument = cats.parse(TestUtils.getUpdate("cats"));
        assertTrue(sendDocument instanceof SendPhoto);
    }

}