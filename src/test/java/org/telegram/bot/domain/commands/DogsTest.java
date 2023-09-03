package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DogsTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;
    @Mock
    private RestTemplate botRestTemplate;

    @InjectMocks
    private Dogs dogs;

    @Test
    void parseWithParamsTest() {
        Update update = TestUtils.getUpdateFromGroup("dogs test");
        PartialBotApiMethod<?> method = dogs.parse(update);
        assertNull(method);
        verify(bot, never()).sendUploadPhoto(anyLong());
    }

    @Test
    void parseWithNoResponseTest() {
        Update update = TestUtils.getUpdateFromGroup("dogs");
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any()))
                .thenThrow(new RestClientException("no_response"));
        assertThrows(BotException.class, () -> dogs.parse(update));
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithEmptyUrlTest() {
        Update update = TestUtils.getUpdateFromGroup("dogs");
        Dogs.Dog dog = new Dogs.Dog();
        ResponseEntity<Dogs.Dog> response = new ResponseEntity<>(dog, HttpStatus.valueOf(200));

        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any())).thenReturn(response);

        assertThrows(BotException.class, () -> dogs.parse(update));
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithJpgResponseTest() {
        Update update = TestUtils.getUpdateFromGroup("dogs");
        Dogs.Dog dog = new Dogs.Dog().setUrl("123.jpg");
        ResponseEntity<Dogs.Dog> response = new ResponseEntity<>(dog, HttpStatus.valueOf(200));

        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any())).thenReturn(response);

        PartialBotApiMethod<?> method = dogs.parse(update);
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        TestUtils.checkDefaultSendPhotoParams(method);
    }

    @Test
    void parseWithNotJpgResponseTest() {
        Update update = TestUtils.getUpdateFromGroup("dogs");
        Dogs.Dog dog = new Dogs.Dog().setUrl("123.mp4");
        ResponseEntity<Dogs.Dog> response = new ResponseEntity<>(dog, HttpStatus.valueOf(200));

        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any())).thenReturn(response);

        PartialBotApiMethod<?> method = dogs.parse(update);
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        TestUtils.checkDefaultSendDocumentParams(method);
    }

}