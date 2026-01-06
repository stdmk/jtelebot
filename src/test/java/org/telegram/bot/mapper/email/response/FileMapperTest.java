package org.telegram.bot.mapper.email.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.bot.services.BotStats;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileMapperTest {

    @Mock
    private Bot bot;
    @Mock
    private BotStats botStats;

    @InjectMocks
    private FileMapper fileMapper;

    @Test
    void toFilesNullableTest() {
        List<File> attachments = fileMapper.toFiles(null);
        assertNull(attachments);
    }

    @Test
    void toFilesTest() throws TelegramApiException, IOException {
        final byte[] attachmentBytes = "attachmentBytes".getBytes(StandardCharsets.UTF_8);
        final byte[] attachmentFileId2Bytes = "attachmentFileId2Byte".getBytes(StandardCharsets.UTF_8);
        final String fileId1 = "fileId1";
        final String fileId2 = "fileId2";
        final String exceptionErrorMessage = "error";

        Attachment attachmentBytesFile = new Attachment()
                .setFile(attachmentBytes)
                .setMimeType("application")
                .setName("name1")
                .setText("text1");
        Attachment attachmentFileId1 = new Attachment()
                .setFileId(fileId1)
                .setMimeType("application")
                .setName("name2")
                .setText("text2");
        Attachment attachmentFileId2 = new Attachment()
                .setFileId(fileId2)
                .setMimeType("application")
                .setName("name3")
                .setText("text3");
        List<Attachment> attachments = List.of(attachmentBytesFile, attachmentFileId1, attachmentFileId2);

        TelegramApiException exception = new TelegramApiException(exceptionErrorMessage);
        when(bot.getBytesTelegramFile(fileId1)).thenThrow(exception);
        when(bot.getBytesTelegramFile(fileId2)).thenReturn(attachmentFileId2Bytes);

        List<File> files = fileMapper.toFiles(attachments);

        assertNotNull(files);
        assertEquals(attachments.size() - 1, files.size());

        File fileBytesFile = getByName(attachmentBytesFile.getName(), files);
        assertNotNull(fileBytesFile);
        assertEquals(FileType.FILE, fileBytesFile.getFileType());
        assertEquals(attachmentBytes, fileBytesFile.getBytes());
        assertEquals(attachmentBytesFile.getName(), fileBytesFile.getName());
        assertEquals(attachmentBytesFile.getText(), fileBytesFile.getText());

        File fileFileId1 = getByName(attachmentFileId1.getName(), files);
        assertNull(fileFileId1);

        File fileFileId2 = getByName(attachmentFileId2.getName(), files);
        assertNotNull(fileFileId2);
        assertEquals(FileType.FILE, fileFileId2.getFileType());
        assertEquals(attachmentFileId2Bytes, fileFileId2.getBytes());
        assertEquals(attachmentFileId2.getName(), fileFileId2.getName());
        assertEquals(attachmentFileId2.getText(), fileFileId2.getText());
    }

    private File getByName(String name, List<File> files) {
        return files.stream().filter(file -> name.equals(file.getName())).findFirst().orElse(null);
    }

}