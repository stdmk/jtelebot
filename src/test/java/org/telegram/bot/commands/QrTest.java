package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrTest {

    @Mock
    private Bot bot;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private BotStats botStats;

    @InjectMocks
    private Qr qr;

    @Test
    void parseWithoutArgumentsTest() {
        final String expectedResponseText = "${command.qr.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup("qr");
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());

        BotResponse botResponse = qr.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
        verify(commandWaitingService).add(message, Qr.class);
    }

    @Test
    void parseTest() throws IOException {
        final String argument = "test";
        BotRequest request = TestUtils.getRequestFromGroup("qr " + argument);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());

        BotResponse botResponse = qr.parse(request).get(0);
        FileResponse fileResponse = TestUtils.checkDefaultFileResponseParams(botResponse, FileType.IMAGE);
        assertEquals(argument, fileResponse.getText());

        File file = fileResponse.getFiles().get(0);
        assertEquals(FileType.IMAGE, file.getFileType());
        assertEquals("qr", file.getName());

        byte[] bytes = file.getInputStream().readAllBytes();
        assertNotNull(bytes);

        verify(bot).sendUploadPhoto(message.getChatId());
    }

    @Test
    void analyzeWithoutAttachmentsTest() {
        BotRequest botRequest = TestUtils.getRequestFromGroup();
        List<BotResponse> botResponseList = qr.analyze(botRequest);
        assertTrue(botResponseList.isEmpty());
    }

    @Test
    void analyzeWithNotPhotoAttachmentTest() {
        BotRequest botRequest = TestUtils.getRequestWithVoice();
        List<BotResponse> botResponseList = qr.analyze(botRequest);
        assertTrue(botResponseList.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("provideTelegramExceptions")
    void analyzeRequestWithTelegramException(Exception exception, String expectedText) throws TelegramApiException, IOException {
        Attachment attachment = TestUtils.getDocument();
        BotRequest request = TestUtils.getRequestFromGroup("");
        Message message = request.getMessage();
        message.setAttachments(List.of(attachment));
        message.setMessageContentType(MessageContentType.PHOTO);

        when(bot.getInputStreamFromTelegramFile(anyString())).thenThrow(exception);

        List<BotResponse> botResponseList = qr.analyze(request);
        assertTrue(botResponseList.isEmpty());

        verify(botStats).incrementErrors(request, exception, expectedText);
    }

    private static Stream<Arguments> provideTelegramExceptions() {
        return Stream.of(
                Arguments.of(new TelegramApiException(""), "Failed to get file from telegram"),
                Arguments.of(new IOException(""), "Failed to read image")
        );
    }

    @Test
    void analyzeNotQrTest() throws IOException, TelegramApiException {
        final String fileId = "fileId";
        final byte[] attachment = TestUtils.getResourceAsBytes("qr/empty.png");
        BotRequest request = TestUtils.getRequestFromGroup();
        Message message = request.getMessage();
        message.setMessageContentType(MessageContentType.PHOTO);
        message.setAttachments(List.of(new Attachment().setFileId(fileId)));

        when(bot.getInputStreamFromTelegramFile(fileId)).thenReturn(new ByteArrayInputStream(attachment));

        List<BotResponse> botResponseList = qr.analyze(request);
        assertTrue(botResponseList.isEmpty());
    }

    @Test
    void analyzeTest() throws IOException, TelegramApiException {
        final String expectedResponseText = "QR_CODE: <code>test</code>\n";
        final String fileId = "fileId";
        final byte[] attachment = TestUtils.getResourceAsBytes("qr/qr.png");
        BotRequest request = TestUtils.getRequestFromGroup();
        Message message = request.getMessage();
        message.setMessageContentType(MessageContentType.PHOTO);
        message.setAttachments(List.of(new Attachment().setFileId(fileId)));

        when(bot.getInputStreamFromTelegramFile(fileId)).thenReturn(new ByteArrayInputStream(attachment));

        BotResponse botResponse = qr.analyze(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

}