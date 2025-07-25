package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetadataTest {

    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private Bot bot;
    @Mock
    private BotStats botStats;

    @InjectMocks
    private Metadata metadata;

    @Test
    void parseWithParamsTest() {
        BotRequest request = TestUtils.getRequestFromGroup("metadata test");
        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        List<BotResponse> botResponseList = metadata.parse(request);
        assertTrue(botResponseList.isEmpty());
    }

    @Test
    void parseWithoutFilesTest() {
        BotRequest request = TestUtils.getRequestFromGroup("metadata");

        BotResponse botResponse = metadata.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse, FormattingStyle.HTML);

        verify(bot).sendTyping(request.getMessage().getChatId());
        assertEquals("${command.metadata.commandwaitingstart}", textResponse.getText());
        verify(commandWaitingService).add(any(Message.class), any(Class.class));
    }

    @Test
    void parseFileWithOverLimitFileSizeTest() {
        BotRequest request = TestUtils.getRequestFromGroup("metadata");
        Attachment document = TestUtils.getDocument("mimetype", 20971521L);

        request.getMessage().setAttachments(List.of(document));
        request.getMessage().setMessageContentType(MessageContentType.FILE);

        assertThrows(BotException.class, () -> metadata.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.TOO_BIG_FILE);
    }

    @Test
    void parseFileWithApiExceptionTest() throws TelegramApiException, IOException {
        BotRequest request = TestUtils.getRequestFromGroup("metadata");

        Attachment attachment = TestUtils.getDocument();
        request.getMessage().setAttachments(List.of(attachment));
        request.getMessage().setMessageContentType(MessageContentType.VIDEO);

        when(bot.getInputStreamFromTelegramFile(attachment.getFileId())).thenThrow(new BotException("internal error"));

        assertThrows(BotException.class, () -> metadata.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseFileWithLibExceptionTest() throws TelegramApiException, IOException {
        BotRequest request = TestUtils.getRequestFromGroup("metadata");

        Attachment attachment = TestUtils.getDocument();
        request.getMessage().setAttachments(List.of(attachment));
        request.getMessage().setMessageContentType(MessageContentType.AUDIO);

        InputStream inputStream = mock(InputStream.class);
        when(bot.getInputStreamFromTelegramFile(attachment.getFileId())).thenReturn(inputStream);

        assertThrows(BotException.class, () -> metadata.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseFileWithIOExceptionTest() throws TelegramApiException, IOException {
        Attachment attachment = TestUtils.getDocument();
        BotRequest request = TestUtils.getRequestFromGroup("metadata");
        request.getMessage().setAttachments(List.of(attachment));

        IOException exception = new IOException();
        when(bot.getInputStreamFromTelegramFile(anyString())).thenThrow(exception);

        assertThrows((BotException.class), () -> metadata.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(botStats).incrementErrors(request, exception, "Failed to get metadata from file");
        verify(bot).sendTyping(TestUtils.DEFAULT_CHAT_ID);
    }

    @Test
    void parseFileWithTelegramExceptionTest() throws TelegramApiException, IOException {
        Attachment attachment = TestUtils.getDocument();
        BotRequest request = TestUtils.getRequestFromGroup("metadata");
        request.getMessage().setAttachments(List.of(attachment));

        TelegramApiException exception = new TelegramApiException();
        when(bot.getInputStreamFromTelegramFile(anyString())).thenThrow(exception);

        assertThrows((BotException.class), () -> metadata.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
        verify(botStats).incrementErrors(request, exception, "Failed to get file from telegram");
        verify(bot).sendTyping(TestUtils.DEFAULT_CHAT_ID);
    }

    @Test
    void parseTest() throws IOException, TelegramApiException {
        Attachment attachment = TestUtils.getDocument();
        Message message = TestUtils.getMessage();
        message.setAttachments(List.of(attachment));
        message.setMessageContentType(MessageContentType.PHOTO);

        BotRequest request = TestUtils.getRequestWithRepliedMessage(message);
        InputStream file = new FileInputStream("src/test/resources/png.png");

        when(bot.getInputStreamFromTelegramFile(attachment.getFileId())).thenReturn(file);

        BotResponse botResponse = metadata.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse, FormattingStyle.HTML);

        verify(bot).sendTyping(request.getMessage().getChatId());
        assertNotNull(textResponse.getText());
    }

}