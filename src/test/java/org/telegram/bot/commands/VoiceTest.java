package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.SaluteSpeechVoice;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.SpeechParseException;
import org.telegram.bot.exception.SpeechSynthesizeException;
import org.telegram.bot.providers.sber.SpeechParser;
import org.telegram.bot.providers.sber.impl.SaluteSpeechSynthesizerImpl;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.executors.SendMessageExecutor;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

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
    private NetworkUtils networkUtils;
    @Mock
    private SaluteSpeechSynthesizerImpl speechSynthesizer;
    @Mock
    private SpeechParser speechParser;
    @Mock
    private LanguageResolver languageResolver;
    @Mock
    private SendMessageExecutor sendMessageExecutor;
    @Mock
    private BotStats botStats;
    @Mock
    private Bot bot;

    @InjectMocks
    private Voice voice;

    @Test
    void analyzeUpdateWithoutVoiceTest() throws SpeechParseException {
        Update updateFromGroup = getUpdateFromGroup();
        voice.analyze(updateFromGroup);
        verify(speechParser, never()).parse(any(), anyInt());
    }

    @Test
    void analyzeUpdateWithVoiceAndTelegramApiException() throws TelegramApiException, IOException, SpeechParseException {
        Update updateWithVoice = getUpdateWithVoice();
        when(networkUtils.getFileFromTelegram(DEFAULT_FILE_ID)).thenThrow(new TelegramApiException());

        voice.analyze(updateWithVoice);

        verify(botStats).incrementErrors(any(Update.class), any(TelegramApiException.class), anyString());
        verify(speechParser, never()).parse(any(), anyInt());
    }

    @Test
    void analyzeUpdateWithVoiceAndTelegramIOException() throws TelegramApiException, IOException, SpeechParseException {
        Update updateWithVoice = getUpdateWithVoice();
        when(networkUtils.getFileFromTelegram(DEFAULT_FILE_ID)).thenThrow(new IOException());

        voice.analyze(updateWithVoice);

        verify(botStats).incrementErrors(any(Update.class), any(IOException.class), anyString());
        verify(speechParser, never()).parse(any(), anyInt());
    }

    @Test
    void analyzeUpdateWithVoiceAndSpeechParseException() throws TelegramApiException, IOException, SpeechParseException {
        Update updateWithVoice = getUpdateWithVoice();
        byte[] file = "123".getBytes();
        when(networkUtils.getFileFromTelegram(DEFAULT_FILE_ID)).thenReturn(file);
        when(speechParser.parse(file, DEFAULT_VOICE_DURATION)).thenThrow(new SpeechParseException("error"));

        voice.analyze(updateWithVoice);

        verify(botStats).incrementErrors(any(Update.class), any(SpeechParseException.class), anyString());
        verify(speechParser).parse(file, DEFAULT_VOICE_DURATION);
    }

    @Test
    void analyzeUpdateWithVoiceTest() throws TelegramApiException, IOException, SpeechParseException {
        final String expectedResponse = "response";
        Update updateWithVoice = getUpdateWithVoice();
        byte[] file = "123".getBytes();
        when(networkUtils.getFileFromTelegram(DEFAULT_FILE_ID)).thenReturn(file);
        when(speechParser.parse(file, DEFAULT_VOICE_DURATION)).thenReturn(expectedResponse);

        voice.analyze(updateWithVoice);

        verify(speechParser).parse(file, DEFAULT_VOICE_DURATION);

        ArgumentCaptor<SendMessage> sendMessageArgumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(sendMessageExecutor).executeMethod(sendMessageArgumentCaptor.capture());

        TestUtils.checkDefaultSendMessageParams(sendMessageArgumentCaptor.getValue());
    }

    @Test
    void parseWithoutTextTest() {
        final String expectedText = "${command.voice.commandwaitingstart}\n" +
                "Наталья(ru)\n" +
                "Борис(ru)\n" +
                "Марфа(ru)\n" +
                "Тарас(ru)\n" +
                "Александра(ru)\n" +
                "Сергей(ru)\n" +
                "Kira(en)\n";
        Update updateFromGroup = getUpdateFromGroup();
        BotException botException = assertThrows((BotException.class), () -> voice.parse(updateFromGroup));
        assertEquals(expectedText, botException.getMessage());
    }

    @Test
    void parseWithoutTextInRepliedMessageTest() {
        Update updateFromGroup = getUpdateWithRepliedMessage("");
        assertThrows((BotException.class), () -> voice.parse(updateFromGroup));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWithTextFromRepliedMessageTest() throws SpeechSynthesizeException {
        final String errorText = "error";
        final String expectedText = "test";
        Update updateWithRepliedMessage = getUpdateWithRepliedMessage(expectedText);

        when(languageResolver.getChatLanguageCode(updateWithRepliedMessage)).thenReturn(DEFAULT_LANG);
        when(speechSynthesizer.synthesize(expectedText, DEFAULT_LANG)).thenThrow(new SpeechSynthesizeException(errorText));

        BotException botException = assertThrows((BotException.class), () -> voice.parse(updateWithRepliedMessage));
        assertEquals(errorText, botException.getMessage());

        verify(speechSynthesizer).synthesize(expectedText, DEFAULT_LANG);
    }

    @Test
    void parseText() throws SpeechSynthesizeException {
        final byte[] expectedFile = "test".getBytes();
        Update updateFromGroup = getUpdateFromGroup("voice test");

        when(languageResolver.getChatLanguageCode(updateFromGroup)).thenReturn(DEFAULT_LANG);
        when(speechSynthesizer.synthesize("test", DEFAULT_LANG)).thenReturn(expectedFile);

        SendVoice method = voice.parse(updateFromGroup);
        TestUtils.checkDefaultSendVoiceParams(method);
    }

    @Test
    void parseWithVoiceParameterText() throws SpeechSynthesizeException {
        final byte[] expectedFile = "test".getBytes();
        Update updateFromGroup = getUpdateFromGroup("voice kira test");

        when(languageResolver.getChatLanguageCode(updateFromGroup)).thenReturn(DEFAULT_LANG);
        when(speechSynthesizer.synthesize("test", DEFAULT_LANG, SaluteSpeechVoice.KIN)).thenReturn(expectedFile);

        SendVoice method = voice.parse(updateFromGroup);
        TestUtils.checkDefaultSendVoiceParams(method);
    }

}