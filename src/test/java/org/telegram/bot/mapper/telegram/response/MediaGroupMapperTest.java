package org.telegram.bot.mapper.telegram.response;

import org.junit.jupiter.api.Test;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MediaGroupMapperTest {

    private final MediaGroupMapper mediaGroupMapper = new MediaGroupMapper();

    @Test
    void mapTest() {
        final Long chatId = 123L;
        final String text = "text";
        final Integer replyToMessageId = 12345;

        File file1 = new File(FileType.IMAGE, "url1", "name1");
        File file2 = new File(FileType.IMAGE, "url2", "name2");
        file1.getFileSettings().setSpoiler(true);
        FileResponse fileResponse = new FileResponse()
                .setChatId(chatId)
                .setText(text)
                .setReplyToMessageId(replyToMessageId)
                .addFile(file1).addFile(file2);

        SendMediaGroup sendMediaGroup = (SendMediaGroup) mediaGroupMapper.map(fileResponse);

        assertEquals(chatId.toString(), sendMediaGroup.getChatId());
        assertEquals(replyToMessageId, sendMediaGroup.getReplyToMessageId());

        List<InputMedia> medias = sendMediaGroup.getMedias();
        assertEquals(fileResponse.getFiles().size(), medias.size());

        InputMediaPhoto inputMedia1 = (InputMediaPhoto) medias.get(0);
        assertEquals(file1.getUrl(), inputMedia1.getMedia());
        assertEquals(file1.getName(), inputMedia1.getCaption());
        assertTrue(inputMedia1.getHasSpoiler());

        InputMediaPhoto inputMedia2 = (InputMediaPhoto) medias.get(1);
        assertEquals(file2.getUrl(), inputMedia2.getMedia());
        assertEquals(file2.getName(), inputMedia2.getCaption());
        assertFalse(inputMedia2.getHasSpoiler());
    }

}