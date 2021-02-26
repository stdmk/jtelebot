package org.telegram.bot.timers;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Timer;
import org.telegram.bot.services.TimerService;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class SaveDataTimer extends TimerParent {

    private final Logger log = LoggerFactory.getLogger(SaveDataTimer.class);

    private final TimerService timerService;
    private final BotStats botStats;

    @Override
    @Scheduled(fixedRate = 1800000)
    public void execute() {
        Timer timer = timerService.get("saveDataTimer");
        if (timer == null) {
            log.error("Unable to read timer saveDataTimer. Creating new...");
            timer = new Timer();
            timer.setName("saveDataTimer");
            timer.setLastAlarmDt(LocalDateTime.now());
            timerService.save(timer);
        }

        LocalDateTime dateTimeNow = LocalDateTime.now();
        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusMinutes(30);

        if (dateTimeNow.isAfter(nextAlarm)) {
            botStats.saveStats();

            timer.setLastAlarmDt(dateTimeNow);
            timerService.save(timer);
        }
    }
}
