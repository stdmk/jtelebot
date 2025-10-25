package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebScreenTest {

    private static final String DEVICE = "desktop";
    private static final String DIMENSION = "1350x950";
    private static final String FORMAT = "png";
    private static final String TIMEOUT_MS = "5000";
    private static final String TOKEN = "token";

    @Mock
    private Bot bot;
    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private SpeechService speechService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private BotStats botStats;
    @Mock
    private NetworkUtils networkUtils;

    @InjectMocks
    private WebScreen webScreen;

    @Test
    void parseWithoutTokenTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("webscreen");

        when(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> webScreen.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());
        verify(bot, never()).sendUploadPhoto(request.getMessage().getChatId());
    }

    @Test
    void parseWithWrongArgumentTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("webscreen http#://");
        Message message = request.getMessage();

        when(propertiesConfig.getScreenshotMachineToken()).thenReturn("token");
        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> webScreen.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
    }

    @Test
    void parseWithoutArgumentTest() {
        final String expectedResponseText = "${command.webscreen.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup("webscreen");
        Message message = request.getMessage();

        when(propertiesConfig.getScreenshotMachineToken()).thenReturn("token");
        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());

        BotResponse botResponse = webScreen.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(commandWaitingService).add(message, WebScreen.class);
        verify(bot).sendUploadPhoto(message.getChatId());
    }

    @Test
    void parseWithRepliedMessageButWithoutArgumentTest() {
        final String expectedResponseText = "${command.webscreen.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestWithRepliedMessage("webscreen");
        Message message = request.getMessage();
        message.getReplyToMessage().setText("");

        when(propertiesConfig.getScreenshotMachineToken()).thenReturn("token");
        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());

        BotResponse botResponse = webScreen.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(commandWaitingService).add(message, WebScreen.class);
        verify(bot).sendUploadPhoto(message.getChatId());
    }

    @Test
    void parseWithRepliedMessageButNoResponseTest() throws IOException {
        final String expectedErrorText = "error";
        final String url = "http://example.com";
        final String expectedUrl = "https://api.screenshotmachine.com?" +
                "device=" + DEVICE + "&dimension=" + DIMENSION + "&format=" + FORMAT + "&cacheLimit=0&timeout=" + TIMEOUT_MS + "&key=" + TOKEN + "&url=" + url;
        BotRequest request = TestUtils.getRequestWithRepliedMessage("webscreen");
        Message message = request.getMessage();
        message.getReplyToMessage().setText("tratatam " + url + " tratatam http://example2.com");

        when(propertiesConfig.getScreenshotMachineDevice()).thenReturn(DEVICE);
        when(propertiesConfig.getScreenshotMachineDimension()).thenReturn(DIMENSION);
        when(propertiesConfig.getScreenshotMachineFormat()).thenReturn(FORMAT);
        when(propertiesConfig.getScreenshotMachineTimeoutMs()).thenReturn(TIMEOUT_MS);
        when(propertiesConfig.getScreenshotMachineToken()).thenReturn(TOKEN);
        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        IOException ioException = new IOException();
        when(networkUtils.getFileFromUrlWithLimit(expectedUrl)).thenThrow(ioException);
        when(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> webScreen.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        ArgumentCaptor<URL> urlArgumentCaptor = ArgumentCaptor.forClass(URL.class);
        verify(botStats).incrementErrors(urlArgumentCaptor.capture(), eq(ioException), eq("Error getting screen"));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
    }

    @Test
    void parseTest() throws IOException {
        final String url = "http://example.com";
        final String expectedUrl = "https://api.screenshotmachine.com?" +
                "device=" + DEVICE + "&dimension=" + DIMENSION + "&format=" + FORMAT + "&cacheLimit=0&timeout=" + TIMEOUT_MS + "&key=" + TOKEN + "&url=" + url;
        BotRequest request = TestUtils.getRequestFromGroup("webscreen " + url);
        Message message = request.getMessage();

        when(propertiesConfig.getScreenshotMachineDevice()).thenReturn(DEVICE);
        when(propertiesConfig.getScreenshotMachineDimension()).thenReturn(DIMENSION);
        when(propertiesConfig.getScreenshotMachineFormat()).thenReturn(FORMAT);
        when(propertiesConfig.getScreenshotMachineTimeoutMs()).thenReturn(TIMEOUT_MS);
        when(propertiesConfig.getScreenshotMachineToken()).thenReturn(TOKEN);
        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        InputStream screen = mock(InputStream.class);
        when(networkUtils.getFileFromUrlWithLimit(expectedUrl)).thenReturn(screen);

        BotResponse botResponse = webScreen.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseParams(botResponse, FileType.IMAGE);
        assertEquals(screen, fileResponse.getFiles().get(0).getInputStream());

        verify(botStats).incrementScreenshots();
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
    }

}