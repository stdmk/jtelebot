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

import static org.telegram.bot.utils.DateUtils.atStartOfDay;

@Component
@AllArgsConstructor
public class WolframRequestsTimer extends TimerParent {

    private final Logger log = LoggerFactory.getLogger(WolframRequestsTimer.class);

    private final TimerService timerService;
    private final BotStats botStats;

    @Override
    @Scheduled(fixedRate = 86400000)
    public void execute() {
        Timer timer = timerService.get("wolframRequestsTimer");
        if (timer == null) {
            log.error("Unable to read timer wolframRequestsTimer. Creating new...");
            timer = new Timer();
            timer.setName("wolframRequestsTimer");
            timer.setLastAlarmDt(LocalDateTime.now());
            timerService.save(timer);
        }

        LocalDateTime dateTimeNow = LocalDateTime.now();
        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusMonths(1);

        if (dateTimeNow.isAfter(nextAlarm)) {
            botStats.resetWolframRequests();

            timer.setLastAlarmDt(atStartOfDay(dateTimeNow.plusMonths(1)));
            timerService.save(timer);
        }
    }
}
