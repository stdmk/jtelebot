package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.bot.repositories.DbBackuper;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupTest {

    @Mock
    private Bot bot;
    @Mock
    private DbBackuper dbBackuper;

    @InjectMocks
    private Backup backup;

    @Test
    void parseTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        when(dbBackuper.getDbBackup()).thenReturn(mock(File.class));

        BotResponse botResponse = backup.parse(request).get(0);
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
        FileResponse fileResponse = TestUtils.checkDefaultFileResponseParams(botResponse);

        assertNotNull(fileResponse);
        assertNotNull(fileResponse.getChatId());
        assertFalse(fileResponse.getResponseSettings().isNotification());

        List<org.telegram.bot.domain.model.response.File> files = fileResponse.getFiles();
        assertNotNull(files);
        assertFalse(files.isEmpty());

        org.telegram.bot.domain.model.response.File file = files.get(0);
        assertEquals(FileType.FILE, file.getFileType());
    }
}