package org.telegram.bot.commands;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class XmlTest {

    private final SpeechService speechService = mock(SpeechService.class);
    private final Bot bot = mock(Bot.class);
    private final BotStats botStats = mock(BotStats.class);

    private final Xml xml = new Xml(new XmlMapper(), speechService, bot, botStats);

    @BeforeEach
    void postConstruct() {
        ReflectionTestUtils.invokeMethod(xml, "postConstruct");
    }

    @Test
    void parseWithoutArgumentTest() {
        final String expectedError = "error";
        BotRequest request = TestUtils.getRequestFromGroup("xml");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedError);

        BotException botException = assertThrows((BotException.class), () -> xml.parse(request));

        assertEquals(expectedError, botException.getMessage());
    }

    @Test
    void parseWithoutArgumentWithVoiceTest() {
        final String expectedError = "error";
        BotRequest request = TestUtils.getRequestFromGroup("xml");
        Message message = request.getMessage();
        message.setMessageContentType(MessageContentType.AUDIO);
        message.setAttachments(List.of(new Attachment()));

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedError);

        BotException botException = assertThrows((BotException.class), () -> xml.parse(request));

        assertEquals(expectedError, botException.getMessage());
    }

    @Test
    void parseCorruptedXmlTest() {
        final String expectedResponseText = "`Unexpected end of input block in end tag" + System.lineSeparator() +
                " at [row,col {unknown-source}]: [1,17]\n" +
                " at [Source: (StringReader); line: 1, column: 18]`";
        BotRequest request = TestUtils.getRequestFromGroup("xml <test>value</test");

        BotResponse botResponse = xml.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseCompressedXmlTest() {
        final String expectedResponseText = "```xml\n<test>" + System.lineSeparator() +
                "  <test2>value</test2>" + System.lineSeparator() +
                "</test>" + System.lineSeparator() +
                "```";
        BotRequest request = TestUtils.getRequestFromGroup("xml <test><test2>value</test2></test>");

        BotResponse botResponse = xml.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseBeautyXmlTest() {
        final String expectedResponseText = "```xml\n<test><test2>value</test2></test>```";
        BotRequest request = TestUtils.getRequestFromGroup("xml <test>\n<test2>value</test2>\n</test>");

        BotResponse botResponse = xml.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseFailedGetFileTest() throws TelegramApiException, IOException {
        final String expectedError = "error";
        final String fileId = "fileId";
        BotRequest request = TestUtils.getRequestFromGroup("xml");
        Message message = request.getMessage();
        message.setAttachments(List.of(new Attachment().setFileId(fileId)));
        message.setMessageContentType(MessageContentType.FILE);

        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedError);
        TelegramApiException exception = new TelegramApiException();
        when(bot.getInputStreamFromTelegramFile(fileId)).thenThrow(exception);

        BotException botException = assertThrows((BotException.class), () -> xml.parse(request));

        assertEquals(expectedError, botException.getMessage());

        verify(botStats).incrementErrors(request, exception, "Failed to get file from telegram");
    }

    @Test
    void parseFailedReadFileTest() throws TelegramApiException, IOException {
        final String expectedError = "error";
        final String fileId = "fileId";
        BotRequest request = TestUtils.getRequestFromGroup("xml");
        Message message = request.getMessage();
        message.setAttachments(List.of(new Attachment().setFileId(fileId)));
        message.setMessageContentType(MessageContentType.FILE);

        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedError);
        InputStream inputStream = mock(InputStream.class);
        IOException ioException = new IOException();
        when(inputStream.readAllBytes()).thenThrow(ioException);
        when(bot.getInputStreamFromTelegramFile(fileId)).thenReturn(inputStream);

        BotException botException = assertThrows((BotException.class), () -> xml.parse(request));

        assertEquals(expectedError, botException.getMessage());

        verify(botStats).incrementErrors(request, ioException, "Failed to read bytes of file from telegram");
    }

    @Test
    void parseFileTest() throws TelegramApiException, IOException {
        final String expectedResponseText = "<test><test2>value</test2></test>";
        final String fileName = "fileName";
        final String fileId = "fileId";
        BotRequest request = TestUtils.getRequestFromGroup("xml");
        Message message = request.getMessage();
        message.setAttachments(List.of(new Attachment().setFileId(fileId).setName(fileName)));
        message.setMessageContentType(MessageContentType.FILE);

        InputStream inputStream = mock(InputStream.class);
        when(inputStream.readAllBytes()).thenReturn("<test>\n<test2>value</test2>\n</test>".getBytes());
        when(bot.getInputStreamFromTelegramFile(fileId)).thenReturn(inputStream);

        BotResponse botResponse = xml.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseParams(botResponse);

        File file = fileResponse.getFiles().get(0);
        assertEquals(fileName, file.getName());

        String actualResponseText = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(expectedResponseText, actualResponseText);
    }

}