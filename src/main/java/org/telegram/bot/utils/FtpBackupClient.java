package org.telegram.bot.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.stereotype.Component;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.providers.ftp.FtpClientProvider;
import org.telegram.bot.services.BotStats;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Component
@Slf4j
public class FtpBackupClient {

    private static final String BACK_FILE_PREFIX = "backup_";
    private static final Pattern BACKUP_FILE_NAME_PATTERN = Pattern.compile(BACK_FILE_PREFIX + "\\d{8}");

    private final FtpClientProvider ftpClientProvider;
    private final Clock clock;
    private final BotStats botStats;

    public void process(String url, FileInputStream backup, Integer daysBeforeExpiration, Long maxSizeBytes) {
        FtpConnectionConfig ftpConnectionConfig = new FtpConnectionConfig(toUrl(url));
        FTPClient ftpClient = getFtpClient(ftpConnectionConfig);
        String workingPath = ftpConnectionConfig.getPath();
        String fileParentPath = getFileParentPath(workingPath);

        if (daysBeforeExpiration != null) {
            deleteExpiredFiles(ftpClient, daysBeforeExpiration, fileParentPath, workingPath);
        }

        if (maxSizeBytes != null) {
            deleteOldFilesIfExceedsLimit(ftpClient, maxSizeBytes, fileParentPath, workingPath);
        }

        putBackup(ftpClient, fileParentPath, backup);

        disconnect(ftpClient);
    }

    public void putBackup(FTPClient ftpClient, String fileParentPath, FileInputStream backup) {
        String backupFileName = BACK_FILE_PREFIX + DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now(clock));
        String backupFilePath = fileParentPath + backupFileName;

        try {
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.storeFile(backupFilePath, backup);
        } catch (IOException e) {
            String errorMessage = "Failed to put backup into ftp: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(backupFilePath, errorMessage);
            throw new BotException(errorMessage);
        }
    }

    private void deleteExpiredFiles(FTPClient ftpClient, int daysBeforeExpiration, String fileParentPath, String workingPath) {
        List<FTPFile> ftpFiles = getFtpBackupFiles(ftpClient, workingPath);
        if (ftpFiles.isEmpty()) {
            return;
        }

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(clock.getZone()));
        calendar.setTime(Date.from(clock.instant()));
        calendar.add(Calendar.DAY_OF_MONTH, -daysBeforeExpiration);

        ftpFiles
                .stream()
                .filter(ftpFile -> ftpFile.getTimestamp().before(calendar))
                .forEach(ftpFile -> deleteFile(ftpClient, ftpFile, fileParentPath));
    }

    private void deleteOldFilesIfExceedsLimit(FTPClient ftpClient, long maxFileSize, String fileParentPath, String workingPath) {
        List<FTPFile> ftpFiles = getFtpBackupFiles(ftpClient, workingPath);
        if (ftpFiles.isEmpty()) {
            return;
        }

        long filesSize = getFilesSize(ftpFiles);
        while (filesSize > maxFileSize) {
            FTPFile oldestFile = getOldestFile(ftpFiles);
            if (oldestFile != null) {
                deleteFile(ftpClient, oldestFile, fileParentPath);
            }

            ftpFiles = getFtpBackupFiles(ftpClient, workingPath);
            filesSize = getFilesSize(ftpFiles);
        }

    }

    private long getFilesSize(List<FTPFile> ftpFiles) {
        return ftpFiles.stream().mapToLong(FTPFile::getSize).sum();
    }

    private FTPFile getOldestFile(List<FTPFile> ftpFiles) {
        return ftpFiles.stream().min(Comparator.comparing(FTPFile::getTimestamp)).orElse(null);
    }

    private String getFileParentPath(String path) {
        if (path != null && !path.isEmpty()) {
            return path + "/";
        }

        return "";
    }

    private void deleteFile(FTPClient ftpClient, FTPFile ftpFile, String parentPath) {
        String filePath = parentPath + ftpFile.getName();
        try {
            ftpClient.deleteFile(filePath);
        } catch (IOException e) {
            String errorMessage = "Failed to delete ftp-file: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(filePath, errorMessage);
            throw new BotException(errorMessage);
        }
    }

    private URL toUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            String errorMessage = "Failed to parse ftp-url: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(url, errorMessage);
            throw new BotException(errorMessage);
        }
    }

    private List<FTPFile> getFtpBackupFiles(FTPClient ftpClient, String path) {
        try {
            return Arrays.stream(ftpClient.listFiles(path))
                    .filter(FTPFile::isFile)
                    .filter(ftpFile -> isBackupFileName(ftpFile.getName()))
                    .toList();
        } catch (IOException e) {
            String errorMessage = "Failed to get list of ftp-files for path " + path + " : " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(path, errorMessage);
            throw new BotException(errorMessage);
        }
    }

    private boolean isBackupFileName(String fileName) {
        return BACKUP_FILE_NAME_PATTERN.matcher(fileName).matches();
    }

    private FTPClient getFtpClient(FtpConnectionConfig ftpConnectionConfig) {
        FTPClient ftpClient = ftpClientProvider.getFTPClient();
        try {
            ftpClient.setRemoteVerificationEnabled(false);
            ftpClient.connect(ftpConnectionConfig.getHost(), ftpConnectionConfig.getPort());
            ftpClient.login(ftpConnectionConfig.getLogin(), ftpConnectionConfig.getPassword());
            ftpClient.enterLocalPassiveMode();
        } catch (IOException e) {
            String errorMessage = "Failed to connect to ftp: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(ftpConnectionConfig, errorMessage);
            throw new BotException(errorMessage);
        }

        return ftpClient;
    }

    private void disconnect(FTPClient ftpClient) {
        try {
            ftpClient.logout();
            ftpClient.disconnect();
        } catch (IOException e) {
            String errorMessage = "Failed to close ftp-connection: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(ftpClient.getRemoteAddress(), errorMessage);
            throw new BotException(errorMessage);
        }
    }

    @Getter
    @ToString
    public static class FtpConnectionConfig {
        private final String host;
        private final int port;
        private final String login;
        private final String password;
        private final String path;

        private FtpConnectionConfig(URL url) {
            this.host = url.getHost();
            this.port = (url.getPort() != -1) ? url.getPort() : 21;

            String user = null;
            String pass = null;
            String userInfo = url.getUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                user = parts[0];
                if (parts.length > 1) {
                    pass = parts[1];
                }
            }

            this.login = user;
            this.password = pass;
            this.path = url.getPath();
        }
    }

}
