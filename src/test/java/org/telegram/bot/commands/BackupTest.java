package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.repositories.DbBackuper;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.telegram.bot.TestUtils.getUpdateFromGroup;

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
        Update update = getUpdateFromGroup();
        when(dbBackuper.getDbBackup()).thenReturn(new InputFile());

        SendDocument sendDocument = backup.parse(update).get(0);
        verify(bot).sendUploadDocument(update);

        assertNotNull(sendDocument);
        assertNotNull(sendDocument.getChatId());
        assertNotNull(sendDocument.getDocument());
        assertTrue(sendDocument.getDisableNotification());
    }
}