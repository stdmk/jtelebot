package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.virus.VirusScanApiKeyMissingException;
import org.telegram.bot.exception.virus.VirusScanException;
import org.telegram.bot.exception.virus.VirusScanNoResponseException;
import org.telegram.bot.providers.virus.VirusScanner;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VirusTest {

    @Mock
    private Bot bot;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private NetworkUtils networkUtils;
    @Mock
    private SpeechService speechService;
    @Mock
    private VirusScanner virusScanner;

    @InjectMocks
    private Virus virus;

    @Test
    void parseWithFailedToGetTelegramFileIdTest() {
        Attachment attachment = TestUtils.getDocument();
        BotRequest request = TestUtils.getRequestFromGroup("virus");
        request.getMessage().setAttachments(List.of(attachment));

        when(bot.getInputStreamFromTelegramFile(anyString())).thenThrow(new BotException("internal error"));

        assertThrows((BotException.class), () -> virus.parse(request));
        verify(bot).sendTyping(TestUtils.DEFAULT_CHAT_ID);
    }

    @ParameterizedTest
    @MethodSource("provideVirusScanExceptions")
    void parseScanFileWithVirusScanExceptionTest(VirusScanException exception, BotSpeechTag botSpeechTag) throws TelegramApiException, VirusScanException {
        Attachment attachment = TestUtils.getDocument();
        BotRequest request = TestUtils.getRequestFromGroup("virus");
        request.getMessage().setAttachments(List.of(attachment));

        InputStream inputStream = Mockito.mock(InputStream.class);
        when(bot.getInputStreamFromTelegramFile(anyString())).thenReturn(inputStream);
        when(virusScanner.scan(any(InputStream.class))).thenThrow(exception);

        assertThrows((BotException.class), () -> virus.parse(request));
        verify(speechService).getRandomMessageByTag(botSpeechTag);
        verify(bot).sendTyping(TestUtils.DEFAULT_CHAT_ID);
    }

    @ParameterizedTest
    @MethodSource("provideVirusScanExceptions")
    void parseScanUrlWithVirusScanExceptionTest(VirusScanException exception, BotSpeechTag botSpeechTag) throws VirusScanException {
        BotRequest request = TestUtils.getRequestFromGroup("virus http://example.com");
        when(virusScanner.scan(any(URL.class))).thenThrow(exception);
        assertThrows((BotException.class), () -> virus.parse(request));
        verify(speechService).getRandomMessageByTag(botSpeechTag);
        verify(bot).sendTyping(TestUtils.DEFAULT_CHAT_ID);
    }

    private static Stream<Arguments> provideVirusScanExceptions() {
        return Stream.of(
                Arguments.of(new VirusScanApiKeyMissingException(""), BotSpeechTag.UNABLE_TO_FIND_TOKEN),
                Arguments.of(new VirusScanNoResponseException(""), BotSpeechTag.NO_RESPONSE),
                Arguments.of(new VirusScanException(""), BotSpeechTag.INTERNAL_ERROR)
        );
    }

    @Test
    void parseScanUrlWithMalformedUrlTest() {
        BotRequest request = TestUtils.getRequestFromGroup("virus abv");
        assertThrows((BotException.class), () -> virus.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(TestUtils.DEFAULT_CHAT_ID);
    }

    @Test
    void parseWithoutArgumentsTest() {
        final String expectedResponseText = "${command.virus.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup("virus");

        BotResponse botResponse = virus.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(commandWaitingService).add(request.getMessage(), Virus.class);
        verify(bot).sendTyping(TestUtils.DEFAULT_CHAT_ID);
    }

    @Test
    void parseWithUrlTest() throws VirusScanException {
        final String expectedResponseText = "response_text";
        BotRequest request = TestUtils.getRequestFromGroup("virus http://example.com");

        when(virusScanner.scan(any(URL.class))).thenReturn(expectedResponseText);

        BotResponse botResponse = virus.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(TestUtils.DEFAULT_CHAT_ID);
    }

    @Test
    void parseWithDocumentInRepliedMessageTest() throws TelegramApiException, VirusScanException {
        final String expectedResponseText = "response_text";
        Attachment attachment = TestUtils.getDocument();
        BotRequest request = TestUtils.getRequestWithRepliedMessage("abv");
        request.getMessage().getReplyToMessage().setAttachments(List.of(attachment));

        InputStream inputStream = Mockito.mock(InputStream.class);
        when(bot.getInputStreamFromTelegramFile(anyString())).thenReturn(inputStream);
        when(virusScanner.scan(any(InputStream.class))).thenReturn(expectedResponseText);

        BotResponse botResponse = virus.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(TestUtils.DEFAULT_CHAT_ID);
    }

    @Test
    void parseWithUrlInRepliedMessageTest() throws VirusScanException {
        final String expectedResponseText = "response_text";
        BotRequest request = TestUtils.getRequestWithRepliedMessage("http://example.com");

        when(virusScanner.scan(any(URL.class))).thenReturn(expectedResponseText);

        BotResponse botResponse = virus.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(TestUtils.DEFAULT_CHAT_ID);
    }

}