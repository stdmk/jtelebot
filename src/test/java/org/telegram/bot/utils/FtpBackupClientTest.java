package org.telegram.bot.utils;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.providers.ftp.FtpClientProvider;
import org.telegram.bot.services.BotStats;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FtpBackupClientTest {

    private static final String HOST = "127.0.0.1";
    private static final Integer PORT = 21;
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String PATH = "/backup";
    private static final String URL = "ftp://" + USER + ":" + PASSWORD + "@" + HOST + ":" + PORT + PATH;

    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);

    @Mock
    private FtpClientProvider ftpClientProvider;
    @Mock
    private Clock clock;
    @Mock
    private BotStats botStats;

    @InjectMocks
    private FtpBackupClient ftpBackupClient;

    @Mock
    private FTPClient ftpClient;
    @Mock
    private FileInputStream backup;

    private final FTPFile ftpFile1 = mock(FTPFile.class);
    private final FTPFile ftpFile2 = mock(FTPFile.class);
    private final FTPFile ftpFile3 = mock(FTPFile.class);
    private final FTPFile ftpFile4 = mock(FTPFile.class);
    private final FTPFile ftpFile5 = mock(FTPFile.class);

    private final FTPFile[] ftpFiles = new FTPFile[]{ftpFile1, ftpFile2, ftpFile3, ftpFile4, ftpFile5};

    @Test
    void incorrectUrlTest() {
        final String url = "url";
        final String expectedErrorText = "Failed to parse ftp-url: no protocol: " + url;

        BotException botException = assertThrows((BotException.class), () -> ftpBackupClient.process(url, backup, null, null));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(botStats).incrementErrors(url, expectedErrorText);
    }

    @Test
    void processWithConnectExceptionTest() throws IOException {
        final String error = "error";
        final String expectedErrorText = "Failed to connect to ftp: " + error;
        when(ftpClientProvider.getFTPClient()).thenReturn(ftpClient);
        doThrow(new IOException(error)).when(ftpClient).connect(HOST, PORT);

        BotException botException = assertThrows((BotException.class), () -> ftpBackupClient.process(URL, backup, null, null));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(botStats).incrementErrors(any(FtpBackupClient.FtpConnectionConfig.class), eq(expectedErrorText));
    }

    @Test
    void processWithLoginExceptionTest() throws IOException {
        final String error = "error";
        final String expectedErrorText = "Failed to connect to ftp: " + error;
        when(ftpClientProvider.getFTPClient()).thenReturn(ftpClient);
        doThrow(new IOException(error)).when(ftpClient).login(USER, PASSWORD);

        BotException botException = assertThrows((BotException.class), () -> ftpBackupClient.process(URL, backup, null, null));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(botStats).incrementErrors(any(FtpBackupClient.FtpConnectionConfig.class), eq(expectedErrorText));
    }

    @Test
    void processDeleteExpiredFileWithGetFileListExceptionTest() throws IOException {
        final String error = "error";
        final String expectedErrorText = "Failed to get list of ftp-files for path " + PATH + " : " + error;
        when(ftpClientProvider.getFTPClient()).thenReturn(ftpClient);
        doThrow(new IOException(error)).when(ftpClient).listFiles(PATH);

        BotException botException = assertThrows((BotException.class), () -> ftpBackupClient.process(URL, backup, 1, null));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(botStats).incrementErrors(PATH, expectedErrorText);
    }

    @Test
    void processDeleteExpiredFileWithDeleteFileExceptionTest() throws IOException {
        final String error = "error";
        final String expectedErrorText = "Failed to delete ftp-file: " + error;
        final String expectedDeletingFileName = "backup_19991231";

        when(ftpClientProvider.getFTPClient()).thenReturn(ftpClient);
        when(ftpClient.listFiles(PATH)).thenReturn(ftpFiles);
        when(clock.instant()).thenReturn(DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(ftpFile1.isFile()).thenReturn(true);
        when(ftpFile2.isFile()).thenReturn(true);
        when(ftpFile3.isFile()).thenReturn(true);
        when(ftpFile4.isFile()).thenReturn(true);
        when(ftpFile1.getTimestamp()).thenReturn(toCalendar(DATE_TIME.plusDays(1)));
        when(ftpFile2.getTimestamp()).thenReturn(toCalendar(DATE_TIME.plusDays(2)));
        when(ftpFile3.getTimestamp()).thenReturn(toCalendar(DATE_TIME.minusDays(2)));
        when(ftpFile1.getName()).thenReturn("backup_20000101");
        when(ftpFile2.getName()).thenReturn("backup_20000102");
        when(ftpFile3.getName()).thenReturn(expectedDeletingFileName);
        when(ftpFile4.getName()).thenReturn("somefilename");
        when(ftpClient.deleteFile(PATH + "/" + expectedDeletingFileName)).thenThrow(new IOException(error));

        BotException botException = assertThrows((BotException.class), () -> ftpBackupClient.process(URL, backup, 1, null));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(botStats).incrementErrors(PATH + "/" + expectedDeletingFileName, expectedErrorText);
    }

    @Test
    void processDeleteExpiredFileWithDeleteFileTest() throws IOException {
        final String file1Name = "backup_20000101";
        final String file2Name = "backup_20000102";
        final String file3Name = "backup_19991231";
        final String file4Name = "somefilename";

        when(ftpClientProvider.getFTPClient()).thenReturn(ftpClient);
        when(ftpClient.listFiles(PATH)).thenReturn(ftpFiles);
        when(clock.instant()).thenReturn(DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(ftpFile1.isFile()).thenReturn(true);
        when(ftpFile2.isFile()).thenReturn(true);
        when(ftpFile3.isFile()).thenReturn(true);
        when(ftpFile4.isFile()).thenReturn(true);
        when(ftpFile1.getTimestamp()).thenReturn(toCalendar(DATE_TIME.plusDays(1)));
        when(ftpFile2.getTimestamp()).thenReturn(toCalendar(DATE_TIME.plusDays(2)));
        when(ftpFile3.getTimestamp()).thenReturn(toCalendar(DATE_TIME.minusDays(2)));
        when(ftpFile1.getName()).thenReturn(file1Name);
        when(ftpFile2.getName()).thenReturn(file2Name);
        when(ftpFile3.getName()).thenReturn(file3Name);
        when(ftpFile4.getName()).thenReturn(file4Name);

        ftpBackupClient.process(URL, backup, 1, null);

        verify(ftpClient).deleteFile(PATH + "/" + file3Name);
        verify(ftpClient, never()).deleteFile(PATH + "/" + file1Name);
        verify(ftpClient, never()).deleteFile(PATH + "/" + file2Name);
        verify(ftpClient, never()).deleteFile(PATH + "/" + file4Name);

        verify(ftpFile5, never()).getName();
    }

    @Test
    void processDeleteOldFileWithDeleteFileExceptionTest() throws IOException {
        final String error = "error";
        final String expectedErrorText = "Failed to delete ftp-file: " + error;
        final String expectedDeletingFileName = "backup_19991231";
        final String file1Name = "backup_20000101";
        final String file2Name = "backup_20000102";
        final String file3Name = "backup_19991231";
        final String file4Name = "somefilename";

        when(ftpClientProvider.getFTPClient()).thenReturn(ftpClient);
        when(ftpClient.listFiles(PATH)).thenReturn(ftpFiles);
        when(ftpFile1.isFile()).thenReturn(true);
        when(ftpFile2.isFile()).thenReturn(true);
        when(ftpFile3.isFile()).thenReturn(true);
        when(ftpFile4.isFile()).thenReturn(true);
        when(ftpFile1.getTimestamp()).thenReturn(toCalendar(DATE_TIME.plusDays(1)));
        when(ftpFile2.getTimestamp()).thenReturn(toCalendar(DATE_TIME.plusDays(2)));
        when(ftpFile3.getTimestamp()).thenReturn(toCalendar(DATE_TIME.minusDays(2)));
        when(ftpFile1.getSize()).thenReturn(400L);
        when(ftpFile2.getSize()).thenReturn(400L);
        when(ftpFile3.getSize()).thenReturn(400L);
        when(ftpFile1.getName()).thenReturn(file1Name);
        when(ftpFile2.getName()).thenReturn(file2Name);
        when(ftpFile3.getName()).thenReturn(file3Name);
        when(ftpFile4.getName()).thenReturn(file4Name);
        when(ftpClient.deleteFile(PATH + "/" + expectedDeletingFileName)).thenThrow(new IOException(error));

        BotException botException = assertThrows((BotException.class), () -> ftpBackupClient.process(URL, backup, null, 1000L));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(botStats).incrementErrors(PATH + "/" + expectedDeletingFileName, expectedErrorText);
    }

    @Test
    void processDeleteOldFileWithDeleteFileTest() throws IOException {
        final String file1Name = "backup_20000101";
        final String file2Name = "backup_20000102";
        final String file3Name = "backup_19991231";
        final String file4Name = "somefilename";

        when(ftpClientProvider.getFTPClient()).thenReturn(ftpClient);
        when(ftpClient.listFiles(PATH)).thenReturn(ftpFiles).thenReturn(new FTPFile[]{ftpFile1, ftpFile2, ftpFile4, ftpFile5});
        when(ftpFile1.isFile()).thenReturn(true);
        when(ftpFile2.isFile()).thenReturn(true);
        when(ftpFile3.isFile()).thenReturn(true);
        when(ftpFile4.isFile()).thenReturn(true);
        when(clock.instant()).thenReturn(DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(ftpFile1.getTimestamp()).thenReturn(toCalendar(DATE_TIME.plusDays(1)));
        when(ftpFile2.getTimestamp()).thenReturn(toCalendar(DATE_TIME.plusDays(2)));
        when(ftpFile3.getTimestamp()).thenReturn(toCalendar(DATE_TIME.minusDays(2)));
        when(ftpFile1.getSize()).thenReturn(400L);
        when(ftpFile2.getSize()).thenReturn(400L);
        when(ftpFile3.getSize()).thenReturn(400L);
        when(ftpFile1.getName()).thenReturn(file1Name);
        when(ftpFile2.getName()).thenReturn(file2Name);
        when(ftpFile3.getName()).thenReturn(file3Name);
        when(ftpFile4.getName()).thenReturn(file4Name);

        ftpBackupClient.process(URL, backup, null, 1000L);

        verify(ftpClient).deleteFile(PATH + "/" + file3Name);
        verify(ftpClient, never()).deleteFile(PATH + "/" + file1Name);
        verify(ftpClient, never()).deleteFile(PATH + "/" + file2Name);
        verify(ftpClient, never()).deleteFile(PATH + "/" + file4Name);

        verify(ftpFile5, never()).getName();
    }

    @Test
    void processPutBackupFileWithExceptionTest() throws IOException {
        final String expectedBackupFilePath = PATH + "/" + "backup_20000101";
        final String error = "error";
        final String expectedErrorText = "Failed to put backup into ftp: " + error;

        when(ftpClientProvider.getFTPClient()).thenReturn(ftpClient);
        when(ftpClient.listFiles(PATH)).thenReturn(new FTPFile[]{});
        when(clock.instant()).thenReturn(DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(ftpClient.storeFile(expectedBackupFilePath, backup)).thenThrow(new IOException(error));

        BotException botException = assertThrows((BotException.class), () -> ftpBackupClient.process(URL, backup, 5, 1000L));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(botStats).incrementErrors(expectedBackupFilePath, expectedErrorText);
        verify(ftpClient, never()).deleteFile(anyString());
    }

    @Test
    void processPutBackupFileWithLogoutExceptionTest() throws IOException {
        final String expectedBackupFilePath = PATH + "/" + "backup_20000101";
        final String error = "error";
        final String expectedErrorText = "Failed to close ftp-connection: " + error;

        when(ftpClientProvider.getFTPClient()).thenReturn(ftpClient);
        when(ftpClient.listFiles(PATH)).thenReturn(new FTPFile[]{});
        when(clock.instant()).thenReturn(DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(ftpClient.logout()).thenThrow(new IOException(error));
        InetAddress inetAddress = mock(InetAddress.class);
        when(ftpClient.getRemoteAddress()).thenReturn(inetAddress);

        BotException botException = assertThrows((BotException.class), () -> ftpBackupClient.process(URL, backup, 5, 1000L));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(ftpClient).storeFile(expectedBackupFilePath, backup);

        verify(botStats).incrementErrors(inetAddress, expectedErrorText);
        verify(ftpClient, never()).deleteFile(anyString());
    }

    @Test
    void processPutBackupFileWithDisconnectExceptionTest() throws IOException {
        final String expectedBackupFilePath = PATH + "/" + "backup_20000101";
        final String error = "error";
        final String expectedErrorText = "Failed to close ftp-connection: " + error;

        when(ftpClientProvider.getFTPClient()).thenReturn(ftpClient);
        when(ftpClient.listFiles(PATH)).thenReturn(new FTPFile[]{});
        when(clock.instant()).thenReturn(DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        doThrow(new IOException(error)).when(ftpClient).disconnect();
        InetAddress inetAddress = mock(InetAddress.class);
        when(ftpClient.getRemoteAddress()).thenReturn(inetAddress);

        BotException botException = assertThrows((BotException.class), () -> ftpBackupClient.process(URL, backup, 5, 1000L));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(ftpClient).storeFile(expectedBackupFilePath, backup);

        verify(botStats).incrementErrors(inetAddress, expectedErrorText);
        verify(ftpClient, never()).deleteFile(anyString());
    }

    @Test
    void processPutBackupFileTest() throws IOException {
        final String expectedBackupFilePath = PATH + "/" + "backup_20000101";
        final String file1Name = "backup_20000101";
        final String file2Name = "backup_20000102";
        final String file3Name = "backup_19991231";
        final String file4Name = "somefilename";

        when(ftpClientProvider.getFTPClient()).thenReturn(ftpClient);
        when(ftpClient.listFiles(PATH)).thenReturn(new FTPFile[]{});
        when(clock.instant()).thenReturn(DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(ftpClientProvider.getFTPClient()).thenReturn(ftpClient);
        when(ftpClient.listFiles(PATH))
                .thenReturn(ftpFiles)
                .thenReturn(new FTPFile[]{ftpFile1, ftpFile2, ftpFile4, ftpFile5})
                .thenReturn(new FTPFile[]{ftpFile2, ftpFile4, ftpFile5});
        when(ftpFile1.isFile()).thenReturn(true);
        when(ftpFile2.isFile()).thenReturn(true);
        when(ftpFile3.isFile()).thenReturn(true);
        when(ftpFile4.isFile()).thenReturn(true);
        when(clock.instant()).thenReturn(DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(ftpFile1.getTimestamp()).thenReturn(toCalendar(DATE_TIME.plusDays(1)));
        when(ftpFile2.getTimestamp()).thenReturn(toCalendar(DATE_TIME.plusDays(2)));
        when(ftpFile3.getTimestamp()).thenReturn(toCalendar(DATE_TIME.minusDays(2)));
        when(ftpFile1.getSize()).thenReturn(900L);
        when(ftpFile2.getSize()).thenReturn(900L);
        when(ftpFile1.getName()).thenReturn(file1Name);
        when(ftpFile2.getName()).thenReturn(file2Name);
        when(ftpFile3.getName()).thenReturn(file3Name);
        when(ftpFile4.getName()).thenReturn(file4Name);

        ftpBackupClient.process(URL, backup, 1, 1000L);

        verify(ftpClient).deleteFile(PATH + "/" + file3Name);
        verify(ftpClient).deleteFile(PATH + "/" + file1Name);
        verify(ftpClient, never()).deleteFile(PATH + "/" + file4Name);

        verify(ftpFile5, never()).getName();

        verify(ftpClient).storeFile(expectedBackupFilePath, backup);
    }

    private Calendar toCalendar(LocalDateTime dateTime) {
        ZoneId zoneId = ZoneId.systemDefault();

        Instant instant = dateTime.atZone(zoneId).toInstant();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(Date.from(instant));

        return calendar;
    }

}