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
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.config.PropertiesConfig;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleTranslateTest {

    @Mock
    private Bot bot;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private ResponseEntity<GoogleTranslate.TranslateResult> response;

    @InjectMocks
    private GoogleTranslate googleTranslate;

    @Test
    void translateWithoutTokenTest() {
        BotRequest request = TestUtils.getRequestFromGroup("translate test");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(propertiesConfig.getGoogleTranslateToken()).thenReturn(null);

        assertThrows(BotException.class, () -> googleTranslate.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN);
    }

    @Test
    void translateWithApiUnavailableTest() {
        BotRequest request = TestUtils.getRequestFromGroup("translate test");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(propertiesConfig.getGoogleTranslateToken()).thenReturn("123");
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GoogleTranslate.TranslateResult>>any()))
                .thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> googleTranslate.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void translateWithNullableResponseTest() {
        BotRequest request = TestUtils.getRequestFromGroup("translate test");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(propertiesConfig.getGoogleTranslateToken()).thenReturn("123");
        when(response.getBody()).thenReturn(null);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GoogleTranslate.TranslateResult>>any()))
                .thenReturn(response);

        assertThrows(BotException.class, () -> googleTranslate.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void translateTextFromCommandMessageTest() {
        final String responseText = "тест";
        BotRequest request = TestUtils.getRequestFromGroup("translate тест");
        GoogleTranslate.TranslateResult translateResult = new GoogleTranslate.TranslateResult(responseText);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(propertiesConfig.getGoogleTranslateToken()).thenReturn("123");
        when(response.getBody()).thenReturn(translateResult);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GoogleTranslate.TranslateResult>>any()))
                .thenReturn(response);

        BotResponse botResponse = googleTranslate.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(responseText, textResponse.getText());
    }

    @Test
    void translateTextFromCommandMessageWithTargetLangTest() {
        final String responseText = "тест";
        BotRequest request = TestUtils.getRequestFromGroup("translate af тест");
        GoogleTranslate.TranslateResult translateResult = new GoogleTranslate.TranslateResult(responseText);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(propertiesConfig.getGoogleTranslateToken()).thenReturn("123");
        when(response.getBody()).thenReturn(translateResult);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GoogleTranslate.TranslateResult>>any()))
                .thenReturn(response);

        BotResponse botResponse = googleTranslate.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(responseText, textResponse.getText());
    }

    @Test
    void translateWithEmptyCommandTest() {
        BotRequest request = TestUtils.getRequestFromGroup("translate test");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(null);

        BotResponse botResponse = googleTranslate.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(botResponse);
        verify(commandWaitingService).add(any(Message.class), any(Class.class));
    }

    @Test
    void translateTextFromRepliedMessageWithoutTextTest() {
        BotRequest request = TestUtils.getRequestWithRepliedMessage("");
        request.getMessage().getReplyToMessage().setText(null);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());

        assertThrows(BotException.class, () -> googleTranslate.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void translateTextFromRepliedMessageCaptionTest() {
        final String responseText = "test";
        BotRequest request = TestUtils.getRequestWithRepliedMessage("тест");
        request.getMessage().setText(null);
        request.getMessage().getReplyToMessage().setText("translate тест");
        GoogleTranslate.TranslateResult translateResult = new GoogleTranslate.TranslateResult(responseText);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(propertiesConfig.getGoogleTranslateToken()).thenReturn("123");
        when(response.getBody()).thenReturn(translateResult);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GoogleTranslate.TranslateResult>>any()))
                .thenReturn(response);

        BotResponse botResponse = googleTranslate.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(responseText, textResponse.getText());

        assertEquals(request.getMessage().getReplyToMessage().getMessageId(), textResponse.getReplyToMessageId());
    }

    @Test
    void translateTextFromRepliedMessageTest() {
        final String responseText = "test";
        BotRequest request = TestUtils.getRequestWithRepliedMessage("тест");
        GoogleTranslate.TranslateResult translateResult = new GoogleTranslate.TranslateResult(responseText);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(propertiesConfig.getGoogleTranslateToken()).thenReturn("123");
        when(response.getBody()).thenReturn(translateResult);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GoogleTranslate.TranslateResult>>any()))
                .thenReturn(response);

        BotResponse botResponse = googleTranslate.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(responseText, textResponse.getText());

        assertEquals(request.getMessage().getReplyToMessage().getMessageId(), textResponse.getReplyToMessageId());
    }
}