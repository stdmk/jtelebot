package org.telegram.bot.timers;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.commands.Backup;
import org.telegram.bot.domain.entities.Timer;
import org.telegram.bot.services.TimerService;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;

import static org.telegram.bot.utils.DateUtils.atStartOfDay;

@Component
@AllArgsConstructor
public class BackupTimer extends TimerParent {

    private final Logger log = LoggerFactory.getLogger(BackupTimer.class);

    private final ApplicationContext context;
    private final TimerService timerService;
    private final PropertiesConfig propertiesConfig;
    private final Backup backup;

    @Override
    @Scheduled(fixedRate = 14400000)
    public void execute() {
        Timer timer = timerService.get("backupTimer");
        if (timer == null) {
            log.error("Unable to read timer backupTimer. Creating new...");
            timer = new Timer();
            timer.setName("backupTimer");
            timer.setLastAlarmDt(LocalDateTime.now());
            timerService.save(timer);
        }

        LocalDateTime dateTimeNow = LocalDateTime.now();
        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusDays(1);

        if (dateTimeNow.isAfter(nextAlarm)) {
            Bot bot = (Bot) context.getBean("bot");

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
