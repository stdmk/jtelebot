package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.model.request.*;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.SaluteSpeechVoice;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.speech.SpeechParseException;
import org.telegram.bot.exception.speech.SpeechSynthesizeException;
import org.telegram.bot.exception.speech.SpeechSynthesizeNoApiResponseException;
import org.telegram.bot.exception.speech.TooLongSpeechException;
import org.telegram.bot.providers.sber.SpeechParser;
import org.telegram.bot.providers.sber.impl.SaluteSpeechSynthesizerImpl;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.ObjectCopier;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.telegram.bot.TestUtils.*;

@ExtendWith(MockitoExtension.class)
class VoiceTest {
    
    private static final String DEFAULT_LANG = "en";

    @Mock
    private SpeechService speechService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private CommandPropertiesService commandPropertiesService;
    @Mock
    private SaluteSpeechSynthesizerImpl speechSynthesizer;
    @Mock
    private SpeechParser speechParser;
    @Mock
    private LanguageResolver languageResolver;
    @Mock
    private BotStats botStats;
    @Mock
    private Bot bot;
    @Mock
    private ObjectCopier objectCopier;

    @InjectMocks
    private Voice voice;

    @Test
    void analyzeRequestWithoutVoiceTest() throws SpeechParseException {
        BotRequest requestFromGroup = getRequestFromGroup();
        voice.analyze(requestFromGroup);
        verify(speechParser, never()).parse(any(), anyInt());
    }

    @Test
    void analyzeRequestWithVoiceAndTelegramApiExceptionTest() throws SpeechParseException, TelegramApiException, IOException {
        BotRequest requestWithVoice = getRequestWithVoice();
        when(bot.getInputStreamFromTelegramFile(DEFAULT_FILE_ID)).thenThrow(new BotException("internal error"));

        assertThrows(BotException.class, () -> voice.analyze(requestWithVoice));

        verify(speechParser, never()).parse(any(), anyInt());
    }

    @ParameterizedTest
    @MethodSource("provideTelegramExceptions")
    void analyzeRequestWithTelegramExceptionTest(Exception exception) throws TelegramApiException, IOException {
        Attachment attachment = TestUtils.getDocument();
        BotRequest request = TestUtils.getRequestFromGroup("");
        Message message = request.getMessage();
        message.setAttachments(List.of(attachment));
        message.setMessageContentType(MessageContentType.VOICE);

        when(bot.getInputStreamFromTelegramFile(anyString())).thenThrow(exception);

        List<BotResponse> botResponseList = voice.analyze(request);
        assertTrue(botResponseList.isEmpty());

        verify(botStats).incrementErrors(request, exception, "Failed to get file from telegram");
    }

    private static Stream<Exception> provideTelegramExceptions() {
        return Stream.of(
                new TelegramApiException(""),
                new IOException("")
        );
    }

    @Test
    void analyzeRequestWithVoiceAndSpeechParseExceptionTest() throws SpeechParseException, TelegramApiException, IOException {
        BotRequest requestWithVoice = getRequestWithVoice();
        byte[] file = "123".getBytes();

        InputStream inputStream = mock(InputStream.class);
        when(inputStream.readAllBytes()).thenReturn(file);
        when(bot.getInputStreamFromTelegramFile(DEFAULT_FILE_ID)).thenReturn(inputStream);
        when(speechParser.parse(file, DEFAULT_VOICE_DURATION)).thenThrow(new SpeechParseException("error"));

        voice.analyze(requestWithVoice);

        verify(botStats).incrementErrors(any(BotRequest.class), any(SpeechParseException.class), anyString());
        verify(speechParser).parse(file, DEFAULT_VOICE_DURATION);
    }

    @Test
    void analyzeRequestWithVoiceAndTooLongSpeechExceptionPrivateChatTest() throws SpeechParseException, TelegramApiException, IOException {
        final String expectedResponseText = "${command.voice.speechistoolong}";
        BotRequest requestWithVoice = getRequestWithVoice();
        requestWithVoice.getMessage().getChat().setChatId(requestWithVoice.getMessage().getUser().getUserId());
        byte[] file = "123".getBytes();

        InputStream inputStream = mock(InputStream.class);
        when(inputStream.readAllBytes()).thenReturn(file);
        when(bot.getInputStreamFromTelegramFile(DEFAULT_FILE_ID)).thenReturn(inputStream);
        when(speechParser.parse(file, DEFAULT_VOICE_DURATION)).thenThrow(new TooLongSpeechException("error"));

        BotResponse botResponse = voice.analyze(requestWithVoice).get(0);

        TextResponse textResponse = checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void analyzeRequestWithVoiceAndTooLongSpeechExceptionGroupChatTest() throws SpeechParseException, TelegramApiException, IOException {
        BotRequest requestWithVoice = getRequestWithVoice();
        byte[] file = "123".getBytes();

        InputStream inputStream = mock(InputStream.class);
        when(inputStream.readAllBytes()).thenReturn(file);
        when(bot.getInputStreamFromTelegramFile(DEFAULT_FILE_ID)).thenReturn(inputStream);
        when(speechParser.parse(file, DEFAULT_VOICE_DURATION)).thenThrow(new TooLongSpeechException("error"));

        List<BotResponse> botResponses = voice.analyze(requestWithVoice);
        assertTrue(botResponses.isEmpty());
    }

    @Test
    void analyzeRequestWithVoiceTest() throws SpeechParseException, TelegramApiException, IOException {
        final String expectedResponse = "response";
        BotRequest requestWithVoice = getRequestWithVoice();
        byte[] file = "123".getBytes();

        InputStream inputStream = mock(InputStream.class);
        when(inputStream.readAllBytes()).thenReturn(file);
        when(bot.getInputStreamFromTelegramFile(DEFAULT_FILE_ID)).thenReturn(inputStream);
        when(speechParser.parse(file, DEFAULT_VOICE_DURATION)).thenReturn(expectedResponse);

        BotResponse botResponse = voice.analyze(requestWithVoice).get(0);
        TextResponse textResponse = checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponse, textResponse.getText());

        verify(speechParser).parse(file, DEFAULT_VOICE_DURATION);
    }

    @Test
    void analyzeRequestWithVoiceCommandCopyErrorTest() throws SpeechParseException, TelegramApiException, IOException {
        final String expectedResponse = "echo";
        BotRequest requestWithVoice = getRequestWithVoice();
        byte[] file = "123".getBytes();

        InputStream inputStream = mock(InputStream.class);
        when(inputStream.readAllBytes()).thenReturn(file);
        when(bot.getInputStreamFromTelegramFile(DEFAULT_FILE_ID)).thenReturn(inputStream);
        when(speechParser.parse(file, DEFAULT_VOICE_DURATION)).thenReturn(expectedResponse);
        when(bot.getBotUsername()).thenReturn("jtelebot");
        when(commandPropertiesService.findCommandInText(expectedResponse, "jtelebot")).thenReturn(new CommandProperties());

        BotResponse botResponse = voice.analyze(requestWithVoice).get(0);
        TextResponse textResponse = checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponse, textResponse.getText());

        verify(speechParser).parse(file, DEFAULT_VOICE_DURATION);
        verify(bot, never()).processRequestWithoutAnalyze(any(BotRequest.class));
    }

    @Test
    void analyzeRequestWithVoiceCommandTest() throws SpeechParseException, TelegramApiException, IOException {
        final String expectedResponse = "echo";
        final String notNormalizedResponse = "echo .";
        BotRequest requestWithVoice = getRequestWithVoice();
        Message message = requestWithVoice.getMessage();
        byte[] file = "123".getBytes();

        InputStream inputStream = mock(InputStream.class);
        when(inputStream.readAllBytes()).thenReturn(file);
        when(bot.getInputStreamFromTelegramFile(DEFAULT_FILE_ID)).thenReturn(inputStream);
        when(speechParser.parse(file, DEFAULT_VOICE_DURATION)).thenReturn(notNormalizedResponse);
        when(bot.getBotUsername()).thenReturn("jtelebot");
        when(commandPropertiesService.findCommandInText(expectedResponse, "jtelebot")).thenReturn(new CommandProperties());
        when(objectCopier.copyObject(requestWithVoice, BotRequest.class)).thenReturn(requestWithVoice);

        BotResponse botResponse = voice.analyze(requestWithVoice).get(0);
        TextResponse textResponse = checkDefaultTextResponseParams(botResponse);
        assertEquals(notNormalizedResponse, textResponse.getText());

        assertEquals(MessageKind.COMMON, message.getMessageKind());
        assertEquals(MessageContentType.TEXT, message.getMessageContentType());
        assertEquals(expectedResponse, message.getText());

        verify(speechParser).parse(file, DEFAULT_VOICE_DURATION);
        verify(bot).processRequestWithoutAnalyze(any(BotRequest.class));
    }

    @Test
    void parseWithoutTextTest() {
        final String expectedText = """
                ${command.voice.commandwaitingstart}
                Наталья(ru)
                Борис(ru)
                Марфа(ru)
                Тарас(ru)
                Александра(ru)
                Сергей(ru)
                Kira(en)
                """;
        BotRequest requestFromGroup = getRequestFromGroup();
        BotException botException = assertThrows((BotException.class), () -> voice.parse(requestFromGroup));
        assertEquals(expectedText, botException.getMessage());
    }

    @Test
    void parseWithoutTextInRepliedMessageTest() {
        BotRequest requestFromGroup = getRequestWithRepliedMessage("");
        assertThrows((BotException.class), () -> voice.parse(requestFromGroup));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWithTextFromRepliedMessageTest() throws SpeechSynthesizeException {
        final String errorText = "error";
        final String expectedText = "test";
        BotRequest requestWithRepliedMessage = getRequestWithRepliedMessage(expectedText);

        when(languageResolver.getChatLanguageCode(requestWithRepliedMessage)).thenReturn(DEFAULT_LANG);
        when(speechSynthesizer.synthesize(expectedText, DEFAULT_LANG)).thenThrow(new SpeechSynthesizeException(errorText));

        BotException botException = assertThrows((BotException.class), () -> voice.parse(requestWithRepliedMessage));
        assertEquals(errorText, botException.getMessage());

        verify(speechSynthesizer).synthesize(expectedText, DEFAULT_LANG);
    }

    @Test
    void parseWithGettingSpeechSynthesizeNoApiResponseExceptionTest() throws SpeechSynthesizeException {
        final String expectedText = "test";
        BotRequest request = getRequestFromGroup("voice " + expectedText);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(languageResolver.getChatLanguageCode(request)).thenReturn(DEFAULT_LANG);
        when(speechSynthesizer.synthesize(expectedText, DEFAULT_LANG)).thenThrow(new SpeechSynthesizeNoApiResponseException("error"));

        assertThrows((BotException.class), () -> voice.parse(request));

        verify(speechSynthesizer).synthesize(expectedText, DEFAULT_LANG);
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseTextTest() throws SpeechSynthesizeException {
        final byte[] expectedFile = "test".getBytes();
        BotRequest requestFromGroup = getRequestFromGroup("voice test");

        when(commandWaitingService.getText(requestFromGroup.getMessage())).thenReturn(requestFromGroup.getMessage().getCommandArgument());
        when(languageResolver.getChatLanguageCode(requestFromGroup)).thenReturn(DEFAULT_LANG);
        when(speechSynthesizer.synthesize("test", DEFAULT_LANG)).thenReturn(expectedFile);

        BotResponse response = voice.parse(requestFromGroup).get(0);
        TestUtils.checkDefaultFileResponseVoiceParams(response);
    }

    @Test
    void parseWithVoiceParameterTextTest() throws SpeechSynthesizeException {
        final byte[] expectedFile = "test".getBytes();
        BotRequest requestFromGroup = getRequestFromGroup("voice kira test");

        when(commandWaitingService.getText(requestFromGroup.getMessage())).thenReturn(requestFromGroup.getMessage().getCommandArgument());
        when(languageResolver.getChatLanguageCode(requestFromGroup)).thenReturn(DEFAULT_LANG);
        when(speechSynthesizer.synthesize("test", DEFAULT_LANG, SaluteSpeechVoice.KIN)).thenReturn(expectedFile);

        BotResponse response = voice.parse(requestFromGroup).get(0);
        TestUtils.checkDefaultFileResponseVoiceParams(response);

        verify(bot).sendTyping(DEFAULT_CHAT_ID);
    }

}