package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.commands.Backup;
import org.telegram.bot.domain.entities.Timer;
import org.telegram.bot.services.TimerService;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;

import static org.telegram.bot.utils.DateUtils.atStartOfDay;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackupTimer extends TimerParent {

    private final Bot bot;
    private final TimerService timerService;
    private final PropertiesConfig propertiesConfig;
    private final Backup backup;

    @Override
    @Scheduled(fixedRate = 14400000)
    public void execute() {
        Timer timer = timerService.get("backupTimer");
        if (timer == null) {
            log.error("Unable to read timer backupTimer. Creating new...");
            timer = new Timer()
                    .setName("backupTimer")
                    .setLastAlarmDt(LocalDateTime.now());
            timerService.save(timer);
        }

        LocalDateTime dateTimeNow = LocalDateTime.now();
        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusDays(1);

        if (dateTimeNow.isAfter(nextAlarm)) {
            try {
                bot.execute(backup.getDbBackup(propertiesConfig.getAdminId().toString()));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

            timer.setLastAlarmDt(atStartOfDay(dateTimeNow));
            timerService.save(timer);
        }
    }
}
