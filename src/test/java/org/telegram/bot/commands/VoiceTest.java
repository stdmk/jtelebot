package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.SaluteSpeechVoice;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.speech.SpeechParseException;
import org.telegram.bot.exception.speech.SpeechSynthesizeException;
import org.telegram.bot.exception.speech.SpeechSynthesizeNoApiResponseException;
import org.telegram.bot.providers.sber.SpeechParser;
import org.telegram.bot.providers.sber.impl.SaluteSpeechSynthesizerImpl;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
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
    private BotStats botStats;
    @Mock
    private Bot bot;

    @InjectMocks
    private Voice voice;

    @Test
    void analyzerequestWithoutVoiceTest() throws SpeechParseException {
        BotRequest requestFromGroup = getRequestFromGroup();
        voice.analyze(requestFromGroup);
        verify(speechParser, never()).parse(any(), anyInt());
    }

    @Test
    void analyzerequestWithVoiceAndTelegramApiException() throws SpeechParseException {
        BotRequest requestWithVoice = getRequestWithVoice();
        when(bot.getFileFromTelegram(DEFAULT_FILE_ID)).thenThrow(new BotException("internal error"));

        assertThrows(BotException.class, () -> voice.analyze(requestWithVoice));

        verify(speechParser, never()).parse(any(), anyInt());
    }

    @Test
    void analyzerequestWithVoiceAndTelegramIOException() throws SpeechParseException {
        BotRequest requestWithVoice = getRequestWithVoice();
        when(bot.getFileFromTelegram(DEFAULT_FILE_ID)).thenThrow(new BotException("internal error"));

        assertThrows(BotException.class, () -> voice.analyze(requestWithVoice));

        verify(speechParser, never()).parse(any(), anyInt());
    }

    @Test
    void analyzerequestWithVoiceAndSpeechParseException() throws TelegramApiException, IOException, SpeechParseException {
        BotRequest requestWithVoice = getRequestWithVoice();
        byte[] file = "123".getBytes();
        when(bot.getFileFromTelegram(DEFAULT_FILE_ID)).thenReturn(file);
        when(speechParser.parse(file, DEFAULT_VOICE_DURATION)).thenThrow(new SpeechParseException("error"));

        voice.analyze(requestWithVoice);

        verify(botStats).incrementErrors(any(BotRequest.class), any(SpeechParseException.class), anyString());
        verify(speechParser).parse(file, DEFAULT_VOICE_DURATION);
    }

    @Test
    void analyzerequestWithVoiceTest() throws TelegramApiException, IOException, SpeechParseException {
        final String expectedResponse = "response";
        BotRequest requestWithVoice = getRequestWithVoice();
        byte[] file = "123".getBytes();
        when(bot.getFileFromTelegram(DEFAULT_FILE_ID)).thenReturn(file);
        when(speechParser.parse(file, DEFAULT_VOICE_DURATION)).thenReturn(expectedResponse);

        BotResponse botResponse = voice.analyze(requestWithVoice).get(0);
        TextResponse textResponse = checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponse, textResponse.getText());

        verify(speechParser).parse(file, DEFAULT_VOICE_DURATION);

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
    void parseText() throws SpeechSynthesizeException {
        final byte[] expectedFile = "test".getBytes();
        BotRequest requestFromGroup = getRequestFromGroup("voice test");

        when(commandWaitingService.getText(requestFromGroup.getMessage())).thenReturn(requestFromGroup.getMessage().getCommandArgument());
        when(languageResolver.getChatLanguageCode(requestFromGroup)).thenReturn(DEFAULT_LANG);
        when(speechSynthesizer.synthesize("test", DEFAULT_LANG)).thenReturn(expectedFile);

        BotResponse response = voice.parse(requestFromGroup).get(0);
        TestUtils.checkDefaultFileResponseVoiceParams(response);
    }

    @Test
    void parseWithVoiceParameterText() throws SpeechSynthesizeException {
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