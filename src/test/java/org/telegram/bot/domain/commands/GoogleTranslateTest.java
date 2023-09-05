package org.telegram.bot.domain.commands;

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
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

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
        Update update = TestUtils.getUpdateFromGroup("translate test");

        when(propertiesConfig.getGoogleTranslateToken()).thenReturn(null);

        assertThrows(BotException.class, () -> googleTranslate.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN);
    }

    @Test
    void translateWithApiUnavailableTest() {
        Update update = TestUtils.getUpdateFromGroup("translate test");

        when(propertiesConfig.getGoogleTranslateToken()).thenReturn("123");
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GoogleTranslate.TranslateResult>>any()))
                .thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> googleTranslate.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void translateWithNullableResponseTest() {
        Update update = TestUtils.getUpdateFromGroup("translate test");

        when(propertiesConfig.getGoogleTranslateToken()).thenReturn("123");
        when(response.getBody()).thenReturn(null);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GoogleTranslate.TranslateResult>>any()))
                .thenReturn(response);

        assertThrows(BotException.class, () -> googleTranslate.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void translateTextFromCommandMessageTest() {
        final String responseText = "тест";
        Update update = TestUtils.getUpdateFromGroup("translate тест");
        GoogleTranslate.TranslateResult translateResult = new GoogleTranslate.TranslateResult(responseText);

        when(propertiesConfig.getGoogleTranslateToken()).thenReturn("123");
        when(response.getBody()).thenReturn(translateResult);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GoogleTranslate.TranslateResult>>any()))
                .thenReturn(response);

        SendMessage sendMessage = googleTranslate.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage);
        assertEquals(responseText, sendMessage.getText());
    }

    @Test
    void translateTextFromCommandMessageWithTargetLangTest() {
        final String responseText = "тест";
        Update update = TestUtils.getUpdateFromGroup("translate af тест");
        GoogleTranslate.TranslateResult translateResult = new GoogleTranslate.TranslateResult(responseText);

        when(propertiesConfig.getGoogleTranslateToken()).thenReturn("123");
        when(response.getBody()).thenReturn(translateResult);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GoogleTranslate.TranslateResult>>any()))
                .thenReturn(response);

        SendMessage sendMessage = googleTranslate.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage);
        assertEquals(responseText, sendMessage.getText());
    }

    @Test
    void translateWithEmptyCommandTest() {
        Update update = TestUtils.getUpdateFromGroup("translate");

        SendMessage sendMessage = googleTranslate.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage);
        verify(commandWaitingService).add(any(Message.class), any(Class.class));
    }

    @Test
    void translateTextFromRepliedMessageWithoutTextTest() {
        Update update = TestUtils.getUpdateWithRepliedMessage("тест");
        update.getMessage().getReplyToMessage().setText(null);

        assertThrows(BotException.class, () -> googleTranslate.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void translateTextFromRepliedMessageCaptionTest() {
        final String responseText = "test";
        Update update = TestUtils.getUpdateWithRepliedMessage("тест");
        update.getMessage().getReplyToMessage().setText(null);
        update.getMessage().getReplyToMessage().setCaption("translate тест");
        GoogleTranslate.TranslateResult translateResult = new GoogleTranslate.TranslateResult(responseText);

        when(propertiesConfig.getGoogleTranslateToken()).thenReturn("123");
        when(response.getBody()).thenReturn(translateResult);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GoogleTranslate.TranslateResult>>any()))
                .thenReturn(response);

        SendMessage sendMessage = googleTranslate.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage);
        assertEquals(responseText, sendMessage.getText());

        assertEquals(update.getMessage().getReplyToMessage().getMessageId(), sendMessage.getReplyToMessageId());
    }

    @Test
    void translateTextFromRepliedMessageTest() {
        final String responseText = "test";
        Update update = TestUtils.getUpdateWithRepliedMessage("тест");
        GoogleTranslate.TranslateResult translateResult = new GoogleTranslate.TranslateResult(responseText);

        when(propertiesConfig.getGoogleTranslateToken()).thenReturn("123");
        when(response.getBody()).thenReturn(translateResult);
        when(botRestTemplate.getForEntity(anyString(), ArgumentMatchers.<Class<GoogleTranslate.TranslateResult>>any()))
                .thenReturn(response);

        SendMessage sendMessage = googleTranslate.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage);
        assertEquals(responseText, sendMessage.getText());

        assertEquals(update.getMessage().getReplyToMessage().getMessageId(), sendMessage.getReplyToMessageId());
    }
}