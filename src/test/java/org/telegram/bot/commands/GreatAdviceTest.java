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
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

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
        Update update = TestUtils.getUpdateFromGroup("advice test");
        SendMessage sendMessage = greatAdvice.parse(update);
        verify(bot, never()).sendTyping(update.getMessage().getChatId());
        assertNull(sendMessage);
    }

    @Test
    void adviceWithApiUnavailableTest() {
        Update update = TestUtils.getUpdateFromGroup("advice");

        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GreatAdvice.FuckingGreatAdvice>>any()))
                .thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> greatAdvice.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void adviceWithNullableResponseTest() {
        Update update = TestUtils.getUpdateFromGroup("advice");

        when(response.getBody()).thenReturn(null);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GreatAdvice.FuckingGreatAdvice>>any()))
                .thenReturn(response);

        assertThrows(BotException.class, () -> greatAdvice.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void adviceTest() {
        final String responseText = "test";
        Update update = TestUtils.getUpdateFromGroup("advice");
        GreatAdvice.FuckingGreatAdvice fuckingGreatAdvice = new GreatAdvice.FuckingGreatAdvice();
        fuckingGreatAdvice.setText(responseText);

        when(response.getBody()).thenReturn(fuckingGreatAdvice);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GreatAdvice.FuckingGreatAdvice>>any()))
                .thenReturn(response);

        SendMessage sendMessage = greatAdvice.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage);
        assertTrue(sendMessage.getText().contains(responseText));
    }

}