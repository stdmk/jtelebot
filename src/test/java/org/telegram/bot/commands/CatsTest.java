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
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

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
        Update update = getUpdateFromGroup("cats test");
        PartialBotApiMethod<?> method = cats.parse(update);
        verify(bot, never()).sendUploadPhoto(update.getMessage().getChatId());
        assertNull(method);
    }

    @Test
    void parseWithNoResponseTest() {
        Update update = getUpdateFromGroup("cats");
        when(botRestTemplate.getForEntity(anyString(), any())).thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> cats.parse(update));
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithEmptyResponseTest() {
        Update update = getUpdateFromGroup("cats");
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);

        assertThrows(BotException.class, () -> cats.parse(update));
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithGifResponseTest() {
        Update update = getUpdateFromGroup("cats");
        Cats.Cat cat = new Cats.Cat();
        cat.setUrl("url.gif");
        Cats.Cat[] catsArray = {cat};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(catsArray);

        PartialBotApiMethod<?> method = cats.parse(update);
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        checkDefaultSendDocumentParams(method);
    }

    @Test
    void parseWithPhotoResponseTest() {
        Update update = getUpdateFromGroup("cats");
        Cats.Cat cat = new Cats.Cat();
        cat.setUrl("url");
        Cats.Cat[] catsArray = {cat};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(catsArray);

        PartialBotApiMethod<?> method = cats.parse(update);
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        checkDefaultSendPhotoParams(method);
    }

}