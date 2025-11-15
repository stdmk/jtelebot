package org.telegram.bot.timers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.repositories.DbBackuper;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.utils.FtpBackupClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupTimerTest {

    @Mock
    private Bot bot;
    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private DbBackuper dbBackuper;
    @Mock
    private BotStats botStats;
    @Mock
    private FtpBackupClient ftpBackupClient;

    @InjectMocks
    private BackupTimer backupTimer;

    @Test
    void executeWithoutFtpUrlTest() {
        final Long adminId = 12345L;
        File backup = mock(File.class);
        when(dbBackuper.getDbBackup()).thenReturn(backup);
        when(propertiesConfig.getAdminId()).thenReturn(adminId);

        backupTimer.execute();

        ArgumentCaptor<FileResponse> fileResponseArgumentCaptor = ArgumentCaptor.forClass(FileResponse.class);
        verify(bot).sendDocument(fileResponseArgumentCaptor.capture());

        FileResponse response = fileResponseArgumentCaptor.getValue();
        assertNotNull(response);
        assertEquals(adminId, response.getChatId());

        List<org.telegram.bot.domain.model.response.File> files = response.getFiles();
        assertEquals(1, files.size());
        org.telegram.bot.domain.model.response.File file = files.get(0);
        assertNotNull(file);
        assertEquals(FileType.FILE, file.getFileType());
        assertEquals(backup, file.getDiskFile());

        ResponseSettings responseSettings = response.getResponseSettings();
        assertNotNull(responseSettings);
        assertFalse(responseSettings.getNotification());
    }

    @Test
    void executeWithFtpUrlAndExceptionsTest() throws IOException {
        final String error = "error";
        final String expectedErrorMessage = "Failed to backup db: " + error;
        File backup = File.createTempFile("test", ".txt");
        backup.deleteOnExit();

        when(dbBackuper.getDbBackup()).thenReturn(backup);
        when(propertiesConfig.getFtpBackupUrl()).thenReturn("url");
        when(propertiesConfig.getFtpRetryCount()).thenReturn(2);
        doThrow(BotException.class)
                .doThrow(new RuntimeException(error))
                .when(ftpBackupClient).process(anyString(), any(FileInputStream.class), any(), any());

        backupTimer.execute();

        verify(bot).sendDocument(any(FileResponse.class));

        verify(botStats, times(2)).incrementErrors(expectedErrorMessage, expectedErrorMessage);
    }

    @Test
    void executeWithFtpUrlTest() throws IOException {
        final String url = "url";
        final int daysBeforeExpirationBackup = 2;
        final long maxBackupsSizeBytes = 30;

        File backup = File.createTempFile("test", ".txt");
        backup.deleteOnExit();

        when(dbBackuper.getDbBackup()).thenReturn(backup);
        when(propertiesConfig.getFtpBackupUrl()).thenReturn(url);
        when(propertiesConfig.getDaysBeforeExpirationBackup()).thenReturn(daysBeforeExpirationBackup);
        when(propertiesConfig.getMaxBackupsSizeBytes()).thenReturn(maxBackupsSizeBytes);

        backupTimer.execute();

        verify(bot).sendDocument(any(FileResponse.class));

        verify(ftpBackupClient).process(eq(url), any(FileInputStream.class), eq(daysBeforeExpirationBackup), eq(maxBackupsSizeBytes));
    }

}