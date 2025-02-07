package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.repositories.DbBackuper;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackupTimer extends TimerParent {

    private final Bot bot;
    private final PropertiesConfig propertiesConfig;
    private final DbBackuper dbBackuper;
    private final BotStats botStats;

    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void execute() {
        File dbBackup = dbBackuper.getDbBackup();

        bot.sendDocument(new FileResponse()
                .setChatId(propertiesConfig.getAdminId())
                .addFile(new org.telegram.bot.domain.model.response.File(FileType.FILE, dbBackup))
                .setResponseSettings(new ResponseSettings().setNotification(false)));

        if (propertiesConfig.getFtpBackupUrl() != null) {
            putBackupIntoFtp(propertiesConfig.getFtpBackupUrl(), dbBackup);
        }
    }

    private void putBackupIntoFtp(String url, File backup) {
        String backupFileName = "backup_" + DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now());

        try (OutputStream outputStream = new URL(url + "/" + backupFileName).openConnection().getOutputStream()) {
            Files.copy(backup.toPath(), outputStream);
        } catch (IOException e) {
            String errorMessage = "Failed to put backup into ftp: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(url, errorMessage);
            throw new BotException(errorMessage);
        }
    }

}
