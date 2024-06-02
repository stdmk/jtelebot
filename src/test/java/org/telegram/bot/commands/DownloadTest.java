package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.telegram.bot.TestUtils.*;

@ExtendWith(MockitoExtension.class)
class DownloadTest {

    @Mock
    private Bot bot;
    @Mock
    private NetworkUtils networkUtils;
    @Mock
    private SpeechService speechService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private InputStream fileFromUrl;

    @InjectMocks
    private Download download;

    private final static String DEFAULT_FILE_NAME = "file";
    private final static String FILE_NAME = "favicon.ico";
    private final static String URL = "http://example.org/";

    @Test
    void parseWithEmptyArgumentTest() {
        BotRequest request = getRequestFromGroup("download");

        BotResponse response = download.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());
        checkDefaultTextResponseParams(response);
        Mockito.verify(commandWaitingService).add(request.getMessage(), Download.class);
    }

    @Test
    void parseWithTwoWrongArgumentsTest() {
        BotRequest request = getRequestFromGroup("download test test");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        assertThrows(BotException.class, () -> download.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"download " + URL + " " + FILE_NAME, "download " + FILE_NAME + " " + URL})
    void parseWithTwoArgumentsTest(String command) throws Exception {
        BotRequest request = getRequestFromGroup(command);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(networkUtils.getFileFromUrlWithLimit(anyString())).thenReturn(fileFromUrl);

        BotResponse response = download.parse(request).get(0);
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
        FileResponse fileResponse = checkDefaultFileResponseParams(response);

        File file = fileResponse.getFiles().get(0);
        assertEquals(FILE_NAME, file.getName());
    }

    @Test
    void parseWithOneWrongArgument() {
        BotRequest request = getRequestFromGroup("download test");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        assertThrows(BotException.class, () -> download.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWithoutFilenameInUrlTest() throws Exception {
        BotRequest request = getRequestFromGroup("download " + URL);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(networkUtils.getFileFromUrlWithLimit(anyString())).thenReturn(fileFromUrl);

        BotResponse response = download.parse(request).get(0);
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
        assertNotNull(response);
        assertTrue(response instanceof FileResponse);

        FileResponse fileResponse = (FileResponse) response;
        File file = fileResponse.getFiles().get(0);
        assertEquals(DEFAULT_FILE_NAME, file.getName());
    }

    @Test
    void parseWithOneArgumentTest() throws Exception {
        BotRequest request = getRequestFromGroup("download " + URL + FILE_NAME);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(networkUtils.getFileFromUrlWithLimit(anyString())).thenReturn(fileFromUrl);

        BotResponse response = download.parse(request).get(0);
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
        FileResponse fileResponse = checkDefaultFileResponseParams(response);

        File file = fileResponse.getFiles().get(0);
        assertEquals(FILE_NAME, file.getName());
    }

    @Test
    void parseWithLargeFileTest() throws Exception {
        BotRequest request = getRequestFromGroup("download " + URL);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(networkUtils.getFileFromUrlWithLimit(anyString())).thenThrow(new IOException());

        assertThrows(BotException.class, () -> download.parse(request));
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.TOO_BIG_FILE);
    }

}