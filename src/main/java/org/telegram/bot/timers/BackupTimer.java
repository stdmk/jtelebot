package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class BackupTimer extends TimerParent {

    private final Bot bot;
    private final PropertiesConfig propertiesConfig;
    private final DbBackuper dbBackuper;
    private final BotStats botStats;
    private final FtpBackupClient ftpBackupClient;

    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void execute() {
        File dbBackup = dbBackuper.getDbBackup();

        try (FileInputStream fileInputStream = new FileInputStream(dbBackup)) {
            if (propertiesConfig.getFtpBackupUrl() != null) {
                ftpBackupClient.process(
                        propertiesConfig.getFtpBackupUrl(),
                        fileInputStream,
                        propertiesConfig.getDaysBeforeExpirationBackup(), 
                        propertiesConfig.getMaxBackupsSizeBytes());
            }
        } catch (IOException e) {
            String errorMessage = "Failed to backup db: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(errorMessage, errorMessage);
            throw new BotException(errorMessage);
        }

        bot.sendDocument(new FileResponse()
                .setChatId(propertiesConfig.getAdminId())
                .addFile(new org.telegram.bot.domain.model.response.File(FileType.FILE, dbBackup))
                .setResponseSettings(new ResponseSettings().setNotification(false)));
    }

}
