package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;

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
        Update update = getUpdateFromGroup("download");

        PartialBotApiMethod<?> method = download.parse(update);
        checkDefaultSendMessageParams(method);
        Mockito.verify(commandWaitingService).add(update.getMessage(), Download.class);
    }

    @Test
    void parseWithTwoWrongArgumentsTest() {
        Update update = getUpdateFromGroup("download test test");

        assertThrows(BotException.class, () -> download.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"download " + URL + " " + FILE_NAME, "download " + FILE_NAME + " " + URL})
    void parseWithTwoArgumentsTest(String command) throws Exception {
        Update update = getUpdateFromGroup(command);

        when(networkUtils.getFileFromUrlWithLimit(anyString())).thenReturn(fileFromUrl);

        PartialBotApiMethod<?> method = download.parse(update);
        SendDocument sendDocument = checkDefaultSendDocumentParams(method);

        InputFile inputFile = sendDocument.getDocument();
        assertEquals(FILE_NAME, inputFile.getMediaName());
    }

    @Test
    void parseWithOneWrongArgument() {
        Update update = getUpdateFromGroup("download test");

        assertThrows(BotException.class, () -> download.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWithoutFilenameInUrlTest() throws Exception {
        Update update = getUpdateFromGroup("download " + URL);

        when(networkUtils.getFileFromUrlWithLimit(anyString())).thenReturn(fileFromUrl);

        PartialBotApiMethod<?> method = download.parse(update);
        assertNotNull(method);
        assertTrue(method instanceof SendDocument);

        SendDocument sendDocument = (SendDocument) method;
        InputFile inputFile = sendDocument.getDocument();
        assertEquals(DEFAULT_FILE_NAME, inputFile.getMediaName());
    }

    @Test
    void parseWithOneArgumentTest() throws Exception {
        Update update = getUpdateFromGroup("download " + URL + FILE_NAME);

        when(networkUtils.getFileFromUrlWithLimit(anyString())).thenReturn(fileFromUrl);

        PartialBotApiMethod<?> method = download.parse(update);
        SendDocument sendDocument = checkDefaultSendDocumentParams(method);

        InputFile inputFile = sendDocument.getDocument();
        assertEquals(FILE_NAME, inputFile.getMediaName());
    }

    @Test
    void parseWithLargeFileTest() throws Exception {
        Update update = getUpdateFromGroup("download " + URL);

        when(networkUtils.getFileFromUrlWithLimit(anyString())).thenThrow(new IOException());

        assertThrows(BotException.class, () -> download.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.TOO_BIG_FILE);
    }

}