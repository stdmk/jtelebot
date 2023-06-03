package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.commands.Backup;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackupTimer extends TimerParent {

    private final Bot bot;
    private final PropertiesConfig propertiesConfig;
    private final Backup backup;

    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void execute() {
        try {
            bot.execute(backup.getDbBackup(propertiesConfig.getAdminId().toString()));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
