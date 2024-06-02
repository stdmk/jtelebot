package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
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
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GreatAdviceTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    ResponseEntity<GreatAdvice.FuckingGreatAdvice> response;

    @InjectMocks
    private GreatAdvice greatAdvice;

    @Test
    void adviceWithNotEmptyMessageTextTest() {
        BotRequest request = TestUtils.getRequestFromGroup("advice test");
        List<BotResponse> responseList = greatAdvice.parse(request);
        verify(bot, never()).sendTyping(request.getMessage().getChatId());
        assertTrue(responseList.isEmpty());
    }

    @Test
    void adviceWithApiUnavailableTest() {
        BotRequest request = TestUtils.getRequestFromGroup("advice");

        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GreatAdvice.FuckingGreatAdvice>>any()))
                .thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> greatAdvice.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void adviceWithNullableResponseTest() {
        BotRequest request = TestUtils.getRequestFromGroup("advice");

        when(response.getBody()).thenReturn(null);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GreatAdvice.FuckingGreatAdvice>>any()))
                .thenReturn(response);

        assertThrows(BotException.class, () -> greatAdvice.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void adviceTest() {
        final String responseText = "test";
        BotRequest request = TestUtils.getRequestFromGroup("advice");
        GreatAdvice.FuckingGreatAdvice fuckingGreatAdvice = new GreatAdvice.FuckingGreatAdvice();
        fuckingGreatAdvice.setText(responseText);

        when(response.getBody()).thenReturn(fuckingGreatAdvice);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GreatAdvice.FuckingGreatAdvice>>any()))
                .thenReturn(response);

        BotResponse botResponse = greatAdvice.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(textResponse);
        assertTrue(textResponse.getText().contains(responseText));
    }

}