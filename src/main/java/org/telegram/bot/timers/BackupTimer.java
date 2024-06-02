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
import org.telegram.bot.repositories.DbBackuper;

import java.io.File;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackupTimer extends TimerParent {

    private final Bot bot;
    private final PropertiesConfig propertiesConfig;
    private final DbBackuper dbBackuper;

    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void execute() {
        File dbBackup = dbBackuper.getDbBackup();
        bot.sendDocument(new FileResponse()
                .setChatId(propertiesConfig.getAdminId())
                .addFile(new org.telegram.bot.domain.model.response.File(FileType.FILE, dbBackup))
                .setResponseSettings(new ResponseSettings().setNotification(false)));
    }
}
