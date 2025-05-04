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
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoMapperTest {

    @Mock
    private InputFileMapper inputFileMapper;

    @InjectMocks
    private VideoMapper videoMapper;

    @Test
    void toVideoWithoutSpoilerTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer replyToMessageId = 12345;

        File file = new File(FileType.IMAGE, "url1", "name1");
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

        SendVideo sendVideo = (SendVideo) videoMapper.map(fileResponse);

        assertEquals(chatId.toString(), sendVideo.getChatId());
        assertEquals(replyToMessageId, sendVideo.getReplyToMessageId());
        assertEquals(inputFile, sendVideo.getVideo());
        assertNull(sendVideo.getHasSpoiler());
    }

    @Test
    void toVideoWithSpoilerTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer replyToMessageId = 12345;

        File file = new File(FileType.VIDEO, "url1", "name1");
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

        SendVideo sendVideo = (SendVideo) videoMapper.map(fileResponse);

        assertEquals(chatId.toString(), sendVideo.getChatId());
        assertEquals(replyToMessageId, sendVideo.getReplyToMessageId());
        assertEquals(inputFile, sendVideo.getVideo());
        assertTrue(sendVideo.getHasSpoiler());
    }

}