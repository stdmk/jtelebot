package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.exception.SpeechParseException;
import org.telegram.bot.providers.SpeechParser;
import org.telegram.bot.services.executors.SendMessageExecutor;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.telegram.bot.TestUtils.*;

@ExtendWith(MockitoExtension.class)
class VoiceTest {

    @Mock
    private NetworkUtils networkUtils;
    @Mock
    private SpeechParser speechParser;
    @Mock
    private SendMessageExecutor sendMessageExecutor;
    @Mock
    private BotStats botStats;

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

}