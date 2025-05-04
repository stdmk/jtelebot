package org.telegram.bot.mapper.telegram.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentMapperTest {

    @Mock
    private InputFileMapper inputFileMapper;
    @Mock
    private ParseModeMapper parseModeMapper;

    @InjectMocks
    private DocumentMapper documentMapper;

    @Test
    void mapWithoutResponseSettingsTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer replyToMessageId = 12345;

        File file = new File("fileId");
        FileResponse fileResponse = new FileResponse()
                .setChatId(chatId)
                .setText(text)
                .setReplyToMessageId(replyToMessageId)
                .addFile(file);

        InputFile inputFile = new InputFile();
        when(inputFileMapper.toInputFile(file)).thenReturn(inputFile);

        SendDocument sendDocument = (SendDocument) documentMapper.map(fileResponse);

        assertEquals(chatId.toString(), sendDocument.getChatId());
        assertEquals(replyToMessageId, sendDocument.getReplyToMessageId());
        assertEquals(text, sendDocument.getCaption());
        assertNull(sendDocument.getDisableNotification());
        assertNull(sendDocument.getParseMode());
    }

    @Test
    void mapWithParseModeTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer replyToMessageId = 12345;
        final String expectedParseMode = "HTML";

        ResponseSettings responseSettings = new ResponseSettings().setFormattingStyle(FormattingStyle.HTML);
        File file = new File("fileId");
        FileResponse fileResponse = new FileResponse()
                .setChatId(chatId)
                .setText(text)
                .setReplyToMessageId(replyToMessageId)
                .addFile(file)
                .setResponseSettings(responseSettings);

        InputFile inputFile = new InputFile();
        when(inputFileMapper.toInputFile(file)).thenReturn(inputFile);
        when(parseModeMapper.toParseMode(FormattingStyle.HTML)).thenReturn(expectedParseMode);

        SendDocument sendDocument = (SendDocument) documentMapper.map(fileResponse);

        assertEquals(chatId.toString(), sendDocument.getChatId());
        assertEquals(replyToMessageId, sendDocument.getReplyToMessageId());
        assertEquals(text, sendDocument.getCaption());
        assertNull(sendDocument.getDisableNotification());
        assertEquals(expectedParseMode, sendDocument.getParseMode());
    }

    @Test
    void mapWithResponseSettingsTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer replyToMessageId = 12345;
        final String expectedParseMode = "HTML";

        ResponseSettings responseSettings = new ResponseSettings()
                .setNotification(false)
                .setFormattingStyle(FormattingStyle.HTML);
        File file = new File("fileId");
        FileResponse fileResponse = new FileResponse()
                .setChatId(chatId)
                .setText(text)
                .setReplyToMessageId(replyToMessageId)
                .addFile(file)
                .setResponseSettings(responseSettings);

        InputFile inputFile = new InputFile();
        when(inputFileMapper.toInputFile(file)).thenReturn(inputFile);
        when(parseModeMapper.toParseMode(FormattingStyle.HTML)).thenReturn(expectedParseMode);

        SendDocument sendDocument = (SendDocument) documentMapper.map(fileResponse);

        assertEquals(chatId.toString(), sendDocument.getChatId());
        assertEquals(replyToMessageId, sendDocument.getReplyToMessageId());
        assertEquals(text, sendDocument.getCaption());
        assertTrue(sendDocument.getDisableNotification());
        assertEquals(expectedParseMode, sendDocument.getParseMode());
    }

}