package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Timer;
import org.telegram.bot.services.TimerService;

import java.time.LocalDateTime;

import static org.telegram.bot.utils.DateUtils.atStartOfDay;

@Component
@RequiredArgsConstructor
@Slf4j
public class RussianPostRequestsTimer extends TimerParent {

    private final TimerService timerService;
    private final BotStats botStats;

    @Override
    public void execute() {
        Timer timer = timerService.get("russianPostRequestsTimer");
        if (timer == null) {
            log.error("Unable to read timer russianPostRequestsTimer. Creating new...");
            timer = new Timer()
                    .setName("russianPostRequestsTimer")
                    .setLastAlarmDt(LocalDateTime.now());
            timerService.save(timer);
        }

        LocalDateTime dateTimeNow = LocalDateTime.now();
        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusDays(1);

        if (dateTimeNow.isAfter(nextAlarm)) {
            botStats.resetRussianPostRequests();

            timer.setLastAlarmDt(atStartOfDay(dateTimeNow));
            timerService.save(timer);
        }
    }
}