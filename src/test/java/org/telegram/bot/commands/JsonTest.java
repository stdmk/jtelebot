package org.telegram.bot.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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

class JsonTest {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final SpeechService speechService = mock(SpeechService.class);
    private final Bot bot = mock(Bot.class);
    private final BotStats botStats = mock(BotStats.class);

    private final Json json = new Json(jsonMapper, speechService, bot, botStats);

    @Test
    void parseWithoutArgumentTest() {
        final String expectedError = "error";
        BotRequest request = TestUtils.getRequestFromGroup("json");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedError);

        BotException botException = assertThrows((BotException.class), () -> json.parse(request));

        assertEquals(expectedError, botException.getMessage());
    }

    @Test
    void parseWithoutArgumentWithVoiceTest() {
        final String expectedError = "error";
        BotRequest request = TestUtils.getRequestFromGroup("json");
        Message message = request.getMessage();
        message.setMessageContentType(MessageContentType.AUDIO);
        message.setAttachments(List.of(new Attachment()));

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedError);

        BotException botException = assertThrows((BotException.class), () -> json.parse(request));

        assertEquals(expectedError, botException.getMessage());
    }

    @Test
    void parseCorruptedJsonTest() {
        final String expectedResponseText = "`Unexpected end-of-input: expected close marker for Object (start marker at [Source: (String)\"{\"field\":\"value\"\"; line: 1, column: 1])\n" +
                " at [Source: (String)\"{\"field\":\"value\"\"; line: 1, column: 17]`";
        BotRequest request = TestUtils.getRequestFromGroup("json {\"field\":\"value\"");

        BotResponse botResponse = json.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseCompressedJsonTest() {
        final String expectedResponseText = "```json\n{" + System.lineSeparator() +
                "  \"field\" : \"value\"" + System.lineSeparator() +
                "}```";
        BotRequest request = TestUtils.getRequestFromGroup("json {\"field\":\"value\"}");

        BotResponse botResponse = json.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseBeautyJsonTest() {
        final String expectedResponseText = "```json\n{\"field\":\"value\"}```";
        BotRequest request = TestUtils.getRequestFromGroup("json {\n  \"field\" : \"value\"\n}");

        BotResponse botResponse = json.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseFailedGetFileTest() throws TelegramApiException, IOException {
        final String expectedError = "error";
        final String fileId = "fileId";
        BotRequest request = TestUtils.getRequestFromGroup("json");
        Message message = request.getMessage();
        message.setAttachments(List.of(new Attachment().setFileId(fileId)));
        message.setMessageContentType(MessageContentType.FILE);

        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedError);
        TelegramApiException exception = new TelegramApiException();
        when(bot.getInputStreamFromTelegramFile(fileId)).thenThrow(exception);

        BotException botException = assertThrows((BotException.class), () -> json.parse(request));

        assertEquals(expectedError, botException.getMessage());

        verify(botStats).incrementErrors(request, exception, "Failed to get file from telegram");
    }

    @Test
    void parseFailedReadFileTest() throws TelegramApiException, IOException {
        final String expectedError = "error";
        final String fileId = "fileId";
        BotRequest request = TestUtils.getRequestFromGroup("json");
        Message message = request.getMessage();
        message.setAttachments(List.of(new Attachment().setFileId(fileId)));
        message.setMessageContentType(MessageContentType.FILE);

        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedError);
        InputStream inputStream = mock(InputStream.class);
        IOException ioException = new IOException();
        when(inputStream.readAllBytes()).thenThrow(ioException);
        when(bot.getInputStreamFromTelegramFile(fileId)).thenReturn(inputStream);

        BotException botException = assertThrows((BotException.class), () -> json.parse(request));

        assertEquals(expectedError, botException.getMessage());

        verify(botStats).incrementErrors(request, ioException, "Failed to read bytes of file from telegram");
    }

    @Test
    void parseFileTest() throws TelegramApiException, IOException {
        final String expectedResponseText = "{\"field\":\"value\"}";
        final String fileName = "fileName";
        final String fileId = "fileId";
        BotRequest request = TestUtils.getRequestFromGroup("json");
        Message message = request.getMessage();
        message.setAttachments(List.of(new Attachment().setFileId(fileId).setName(fileName)));
        message.setMessageContentType(MessageContentType.FILE);

        InputStream inputStream = mock(InputStream.class);
        when(inputStream.readAllBytes()).thenReturn("{\n  \"field\" : \"value\"\n}".getBytes());
        when(bot.getInputStreamFromTelegramFile(fileId)).thenReturn(inputStream);

        BotResponse botResponse = json.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseParams(botResponse);

        File file = fileResponse.getFiles().get(0);
        assertEquals(fileName, file.getName());

        String actualResponseText = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(expectedResponseText, actualResponseText);
    }

}