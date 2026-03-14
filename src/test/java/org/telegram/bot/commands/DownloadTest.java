package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.yt_dlp.MediaPlatform;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.youtube.YtDlpBigFileException;
import org.telegram.bot.exception.youtube.YtDlpCallException;
import org.telegram.bot.exception.youtube.YtDlpException;
import org.telegram.bot.exception.youtube.YtDlpNoResponseException;
import org.telegram.bot.providers.media.YtDlpProvider;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.telegram.bot.TestUtils.*;

@ExtendWith(MockitoExtension.class)
class DownloadTest {

    private static final byte[] FILE_FROM_URL = "content".getBytes(StandardCharsets.UTF_8);

    @Mock
    private InternationalizationService internationalizationService;
    @Mock
    private Bot bot;
    @Mock
    private NetworkUtils networkUtils;
    @Mock
    private YtDlpProvider ytDlpProvider;
    @Mock
    private SpeechService speechService;
    @Mock
    private CommandWaitingService commandWaitingService;

    @InjectMocks
    private Download download;

    private final static String DEFAULT_FILE_NAME = "file";
    private final static String URL = "http://example.org/";

    @Test
    void parseWithEmptyArgumentTest() {
        BotRequest request = getRequestFromGroup("download");

        BotResponse response = download.parse(request).getFirst();
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
        when(networkUtils.getFileFromUrlWithLimit(anyString())).thenReturn(FILE_FROM_URL);

        BotResponse response = download.parse(request).getFirst();
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
        assertNotNull(response);
        assertInstanceOf(FileResponse.class, response);

        FileResponse fileResponse = (FileResponse) response;
        File file = fileResponse.getFiles().getFirst();
        assertEquals(DEFAULT_FILE_NAME, file.getName());
    }

    @Test
    void parseWithOneArgumentTest() throws Exception {
        BotRequest request = getRequestFromGroup("download " + URL);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(networkUtils.getFileFromUrlWithLimit(anyString())).thenReturn(FILE_FROM_URL);

        BotResponse response = download.parse(request).getFirst();
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
        FileResponse fileResponse = checkDefaultFileResponseParams(response);

        File file = fileResponse.getFiles().getFirst();
        assertEquals(DEFAULT_FILE_NAME, file.getName());
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

    @Test
    void parseWithYoutubeVideoAsArgumentWhenNoResponseTest() throws YtDlpException {
        final String url = "https://youtube.com/shorts/QWERTYUIOP1";
        BotRequest request = getRequestFromGroup("download " + url);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(ytDlpProvider.getVideo(MediaPlatform.YOUTUBE, url)).thenThrow(new YtDlpNoResponseException("error"));

        assertThrows(BotException.class, () -> download.parse(request));
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithYoutubeVideoAsArgumentWhenTooBigFileTest() throws YtDlpException {
        final String url = "https://youtube.com/shorts/QWERTYUIOP1";
        BotRequest request = getRequestFromGroup("download " + url);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(ytDlpProvider.getVideo(MediaPlatform.YOUTUBE, url)).thenThrow(new YtDlpBigFileException("error"));

        assertThrows(BotException.class, () -> download.parse(request));
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.TOO_BIG_FILE);
    }

    @Test
    void parseWithYoutubeVideoAsArgumentWhenFailedToCallYtDlpTest() throws YtDlpException {
        final String url = "https://youtube.com/shorts/QWERTYUIOP1";
        BotRequest request = getRequestFromGroup("download " + url);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(ytDlpProvider.getVideo(MediaPlatform.YOUTUBE, url)).thenThrow(new YtDlpCallException("error"));

        assertThrows(BotException.class, () -> download.parse(request));
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void parseWithYoutubeVideoAsArgumentTest() throws YtDlpException {
        final String url = "https://youtube.com/shorts/QWERTYUIOP1";
        BotRequest request = getRequestFromGroup("download " + url);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        File youtubeFile = new File("fileId");
        when(ytDlpProvider.getVideo(MediaPlatform.YOUTUBE, url)).thenReturn(youtubeFile);

        BotResponse response = download.parse(request).getFirst();

        FileResponse fileResponse = checkDefaultFileResponseParams(response);

        File file = fileResponse.getFiles().getFirst();
        assertNull(file.getName());
        assertEquals(youtubeFile, file);

        verify(bot).sendUploadDocument(request.getMessage().getChatId());
    }

    @Test
    void parseWithYoutubeAudioAsArgumentTest() throws YtDlpException {
        final String url = "https://youtube.com/shorts/QWERTYUIOP1";
        BotRequest request = getRequestFromGroup("download AuDiO " + url);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        File youtubeFile = new File("fileId");
        when(ytDlpProvider.getAudio(MediaPlatform.YOUTUBE, url)).thenReturn(youtubeFile);

        Set<String> audioMediaTypes = Set.of("audio");
        when(internationalizationService.getAllTranslations("command.download.videotype")).thenReturn(Set.of());
        when(internationalizationService.getAllTranslations("command.download.audiotype")).thenReturn(audioMediaTypes);
        ReflectionTestUtils.invokeMethod(download, "postConstruct");

        BotResponse response = download.parse(request).getFirst();

        FileResponse fileResponse = checkDefaultFileResponseParams(response);

        File file = fileResponse.getFiles().getFirst();
        assertNull(file.getName());
        assertEquals(youtubeFile, file);

        verify(bot).sendUploadDocument(request.getMessage().getChatId());
    }

    @Test
    void parseWithAudioArgumentTest() throws YtDlpException {
        final String url = "https://soundcloud.com/group/track";
        BotRequest request = getRequestFromGroup("download " + url);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        File soundCloudFile = new File("fileId");
        when(ytDlpProvider.getAudio(MediaPlatform.SOUNDCLOUD, url)).thenReturn(soundCloudFile);

        BotResponse response = download.parse(request).getFirst();

        FileResponse fileResponse = checkDefaultFileResponseParams(response);

        File file = fileResponse.getFiles().getFirst();
        assertNull(file.getName());
        assertEquals(soundCloudFile, file);

        verify(bot).sendUploadDocument(request.getMessage().getChatId());
    }

}