package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.commands.Backup;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.services.executors.SendDocumentExecutor;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackupTimer extends TimerParent {

    private final SendDocumentExecutor sendDocumentExecutor;
    private final PropertiesConfig propertiesConfig;
    private final Backup backup;

    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void execute() {
        sendDocumentExecutor.executeMethod(backup.getDbBackup(propertiesConfig.getAdminId().toString()));
    }
}
