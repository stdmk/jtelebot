package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.ffmpeg.FfmpegException;
import org.telegram.bot.providers.ffmpeg.FfmpegProvider;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebcamTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;
    @Mock
    private FfmpegProvider ffmpegProvider;
    @Mock
    private CommandWaitingService commandWaitingService;

    @InjectMocks
    private Webcam webcam;

    @Test
    void parseWithoutArgumentTest() {
        final String expectedResponseText = "${command.webcam.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup("webcam");
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());

        BotResponse botResponse = webcam.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(commandWaitingService).add(message, Webcam.class);
        verify(bot).sendTyping(message.getChatId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"url", "http://example.com a", "http://example.com -1", "http://example.com 21"})
    void parseWrongArgumentTest(String argument) {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("webcam " + argument);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> webcam.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());
        verify(bot, never()).sendTyping(message.getChatId());
        verify(bot, never()).sendUploadVideo(message.getChatId());
    }

    @Test
    void parseWithNoResponseException() throws FfmpegException {
        final String expectedErrorText = "error";
        final String url = "http://example.com";
        final String duration = "5";
        BotRequest request = TestUtils.getRequestFromGroup("webcam " + url + " " + duration);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE)).thenReturn(expectedErrorText);
        when(ffmpegProvider.getVideo(url, duration)).thenThrow(new FfmpegException("no response"));

        BotException botException = assertThrows(BotException.class, () -> webcam.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());
        verify(bot, never()).sendTyping(message.getChatId());
        verify(bot).sendUploadVideo(message.getChatId());
    }

    @Test
    void parseTest() throws FfmpegException {
        final File expectedVideoFile = new File("");
        final String url = "http://example.com";
        final String duration = "5";
        BotRequest request = TestUtils.getRequestFromGroup("webcam " + url + " " + duration);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(ffmpegProvider.getVideo(url, duration)).thenReturn(expectedVideoFile);

        BotResponse botResponse = webcam.parse(request).get(0);
        FileResponse fileResponse = TestUtils.checkDefaultFileResponseParams(botResponse, FileType.VIDEO);

        assertEquals(expectedVideoFile, fileResponse.getFiles().get(0).getDiskFile());
    }

}