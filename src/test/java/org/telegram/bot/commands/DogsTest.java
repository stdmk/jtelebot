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
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

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
        BotRequest request = TestUtils.getRequestFromGroup("dogs test");
        List<BotResponse> botResponses = dogs.parse(request);
        assertTrue(botResponses.isEmpty());
        verify(bot, never()).sendUploadPhoto(anyLong());
    }

    @Test
    void parseWithNoResponseTest() {
        BotRequest request = TestUtils.getRequestFromGroup("dogs");
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any()))
                .thenThrow(new RestClientException("no_response"));
        assertThrows(BotException.class, () -> dogs.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithEmptyUrlTest() {
        BotRequest request = TestUtils.getRequestFromGroup("dogs");
        Dogs.Dog dog = new Dogs.Dog();
        ResponseEntity<Dogs.Dog> response = new ResponseEntity<>(dog, HttpStatus.valueOf(200));

        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any())).thenReturn(response);

        assertThrows(BotException.class, () -> dogs.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithJpgResponseTest() {
        BotRequest request = TestUtils.getRequestFromGroup("dogs");
        Dogs.Dog dog = new Dogs.Dog().setUrl("123.jpg");
        ResponseEntity<Dogs.Dog> response = new ResponseEntity<>(dog, HttpStatus.valueOf(200));

        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any())).thenReturn(response);

        BotResponse botResponse = dogs.parse(request).get(0);
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        TestUtils.checkDefaultFileResponseImageParams(botResponse);
    }

    @Test
    void parseWithJpgResponseIgnoreCaseTest() {
        BotRequest request = TestUtils.getRequestFromGroup("dogs");
        Dogs.Dog dog = new Dogs.Dog().setUrl("123.JpG");
        ResponseEntity<Dogs.Dog> response = new ResponseEntity<>(dog, HttpStatus.valueOf(200));

        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any())).thenReturn(response);

        BotResponse botResponse = dogs.parse(request).get(0);
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        TestUtils.checkDefaultFileResponseImageParams(botResponse);
    }

    @Test
    void parseWithWebmResponseTest() {
        BotRequest request = TestUtils.getRequestFromGroup("dogs");
        Dogs.Dog dog = new Dogs.Dog().setUrl("123.webm");
        ResponseEntity<Dogs.Dog> response = new ResponseEntity<>(dog, HttpStatus.valueOf(200));

        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any())).thenReturn(response);

        BotResponse botResponse = dogs.parse(request).get(0);
        verify(bot).sendUploadVideo(request.getMessage().getChatId());
        TestUtils.checkDefaultFileResponseVideoParams(botResponse);
    }

    @Test
    void parseWithUnknownExtensionResponseTest() {
        BotRequest request = TestUtils.getRequestFromGroup("dogs");
        Dogs.Dog dog = new Dogs.Dog().setUrl("123.zip");
        ResponseEntity<Dogs.Dog> response = new ResponseEntity<>(dog, HttpStatus.valueOf(200));

        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<Dogs.Dog>>any())).thenReturn(response);

        BotResponse botResponse = dogs.parse(request).get(0);
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
        TestUtils.checkDefaultFileResponseParams(botResponse);
    }

}