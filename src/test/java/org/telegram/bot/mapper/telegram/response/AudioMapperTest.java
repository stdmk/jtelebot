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
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioMapperTest {

    @Mock
    private InputFileMapper inputFileMapper;

    @InjectMocks
    private AudioMapper audioMapper;

    @Test
    void mapTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer replyToMessageId = 12345;

        File file = new File(FileType.AUDIO, "url1", "name1");
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

        SendAudio sendAudio = (SendAudio) audioMapper.map(fileResponse);

        assertEquals(chatId.toString(), sendAudio.getChatId());
        assertEquals(replyToMessageId, sendAudio.getReplyToMessageId());
        assertEquals(inputFile, sendAudio.getAudio());
    }

}