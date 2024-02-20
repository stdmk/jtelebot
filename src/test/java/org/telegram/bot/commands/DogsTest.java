package org.telegram.bot.commands;

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
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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
        List<PartialBotApiMethod<?>> methods = dogs.parse(update);
        assertTrue(methods.isEmpty());
        verify(bot, never()).sendUploadPhoto(anyLong());
    }

    @Test
    void parseWithNoResponseTest() {
        Update update = TestUtils.getUpdateFromGroup("dogs");
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any()))
                .thenThrow(new RestClientException("no_response"));
        assertThrows(BotException.class, () -> dogs.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithEmptyUrlTest() {
        Update update = TestUtils.getUpdateFromGroup("dogs");
        Dogs.Dog dog = new Dogs.Dog();
        ResponseEntity<Dogs.Dog> response = new ResponseEntity<>(dog, HttpStatus.valueOf(200));

        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any())).thenReturn(response);

        assertThrows(BotException.class, () -> dogs.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithJpgResponseTest() {
        Update update = TestUtils.getUpdateFromGroup("dogs");
        Dogs.Dog dog = new Dogs.Dog().setUrl("123.jpg");
        ResponseEntity<Dogs.Dog> response = new ResponseEntity<>(dog, HttpStatus.valueOf(200));

        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any())).thenReturn(response);

        PartialBotApiMethod<?> method = dogs.parse(update).get(0);
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        TestUtils.checkDefaultSendPhotoParams(method);
    }

    @Test
    void parseWithJpgResponseIgnoreCaseTest() {
        Update update = TestUtils.getUpdateFromGroup("dogs");
        Dogs.Dog dog = new Dogs.Dog().setUrl("123.JpG");
        ResponseEntity<Dogs.Dog> response = new ResponseEntity<>(dog, HttpStatus.valueOf(200));

        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any())).thenReturn(response);

        PartialBotApiMethod<?> method = dogs.parse(update).get(0);
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        TestUtils.checkDefaultSendPhotoParams(method);
    }

    @Test
    void parseWithWebmResponseTest() {
        Update update = TestUtils.getUpdateFromGroup("dogs");
        Dogs.Dog dog = new Dogs.Dog().setUrl("123.webm");
        ResponseEntity<Dogs.Dog> response = new ResponseEntity<>(dog, HttpStatus.valueOf(200));

        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any())).thenReturn(response);

        PartialBotApiMethod<?> method = dogs.parse(update).get(0);
        verify(bot).sendUploadVideo(update.getMessage().getChatId());
        TestUtils.checkDefaultSendVideoParams(method);
    }

    @Test
    void parseWithUnknownExtensionResponseTest() {
        Update update = TestUtils.getUpdateFromGroup("dogs");
        Dogs.Dog dog = new Dogs.Dog().setUrl("123.zip");
        ResponseEntity<Dogs.Dog> response = new ResponseEntity<>(dog, HttpStatus.valueOf(200));

        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any())).thenReturn(response);

        PartialBotApiMethod<?> method = dogs.parse(update).get(0);
        verify(bot).sendUploadDocument(update.getMessage().getChatId());
        TestUtils.checkDefaultSendDocumentParams(method);
    }

}