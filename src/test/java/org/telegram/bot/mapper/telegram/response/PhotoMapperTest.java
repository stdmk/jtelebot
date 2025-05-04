package org.telegram.bot.mapper.telegram.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoMapperTest {

    @Mock
    private InputFileMapper inputFileMapper;
    @Mock
    private ParseModeMapper parseModeMapper;

    @InjectMocks
    private PhotoMapper photoMapper;

    @Test
    void mapWithoutResponseTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer replyToMessageId = 12345;

        File file = new File(FileType.IMAGE, "url1", "name1");
        FileResponse fileResponse = new FileResponse()
                .setChatId(chatId)
                .setText(text)
                .setReplyToMessageId(replyToMessageId)
                .addFile(file);

        InputFile inputFile = new InputFile();
        when(inputFileMapper.toInputFile(file)).thenReturn(inputFile);

        SendPhoto sendPhoto = (SendPhoto) photoMapper.map(fileResponse);

        assertEquals(chatId.toString(), sendPhoto.getChatId());
        assertEquals(replyToMessageId, sendPhoto.getReplyToMessageId());
        assertEquals(inputFile, sendPhoto.getPhoto());
        assertNull(sendPhoto.getDisableNotification());
        assertNull(sendPhoto.getParseMode());
        assertNull(sendPhoto.getHasSpoiler());
    }

    @Test
    void mapWithParseModeTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer replyToMessageId = 12345;
        final String expectedParseMode = "html";

        File file = new File(FileType.IMAGE, "url1", "name1");
        file.getFileSettings().setSpoiler(true);
        ResponseSettings responseSettings = new ResponseSettings()
                .setFormattingStyle(FormattingStyle.HTML);
        FileResponse fileResponse = new FileResponse()
                .setChatId(chatId)
                .setText(text)
                .setReplyToMessageId(replyToMessageId)
                .setResponseSettings(responseSettings)
                .addFile(file);

        InputFile inputFile = new InputFile();
        when(inputFileMapper.toInputFile(file)).thenReturn(inputFile);
        when(parseModeMapper.toParseMode(FormattingStyle.HTML)).thenReturn(expectedParseMode);

        SendPhoto sendPhoto = (SendPhoto) photoMapper.map(fileResponse);

        assertEquals(chatId.toString(), sendPhoto.getChatId());
        assertEquals(replyToMessageId, sendPhoto.getReplyToMessageId());
        assertEquals(inputFile, sendPhoto.getPhoto());
        assertNull(sendPhoto.getDisableNotification());
        assertEquals(expectedParseMode, sendPhoto.getParseMode());
        assertTrue(sendPhoto.getHasSpoiler());
    }

    @Test
    void mapWithResponseSettingsTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer replyToMessageId = 12345;
        final String expectedParseMode = "html";

        File file = new File(FileType.IMAGE, "url1", "name1");
        file.getFileSettings().setSpoiler(true);
        ResponseSettings responseSettings = new ResponseSettings()
                .setNotification(false)
                .setFormattingStyle(FormattingStyle.HTML);
        FileResponse fileResponse = new FileResponse()
                .setChatId(chatId)
                .setText(text)
                .setReplyToMessageId(replyToMessageId)
                .setResponseSettings(responseSettings)
                .addFile(file);

        InputFile inputFile = new InputFile();
        when(inputFileMapper.toInputFile(file)).thenReturn(inputFile);
        when(parseModeMapper.toParseMode(FormattingStyle.HTML)).thenReturn(expectedParseMode);

        SendPhoto sendPhoto = (SendPhoto) photoMapper.map(fileResponse);

        assertEquals(chatId.toString(), sendPhoto.getChatId());
        assertEquals(replyToMessageId, sendPhoto.getReplyToMessageId());
        assertEquals(inputFile, sendPhoto.getPhoto());
        assertTrue(sendPhoto.getDisableNotification());
        assertEquals(expectedParseMode, sendPhoto.getParseMode());
        assertTrue(sendPhoto.getHasSpoiler());
    }

}