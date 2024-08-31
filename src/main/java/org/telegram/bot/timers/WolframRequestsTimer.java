package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Timer;
import org.telegram.bot.services.TimerService;

import java.time.LocalDateTime;

import static org.telegram.bot.utils.DateUtils.atStartOfDay;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty("wolframAlphaToken")
public class WolframRequestsTimer extends TimerParent {

    private final TimerService timerService;
    private final BotStats botStats;

    @Override
    @Scheduled(fixedRate = 86400000)
    public void execute() {
        Timer timer = timerService.get("wolframRequestsTimer");
        if (timer == null) {
            timer = new Timer()
                    .setName("wolframRequestsTimer")
                    .setLastAlarmDt(LocalDateTime.now());
            timerService.save(timer);
        }

        LocalDateTime dateTimeNow = LocalDateTime.now();
        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusMonths(1);

        if (dateTimeNow.isAfter(nextAlarm)) {
            botStats.resetWolframRequests();

            timer.setLastAlarmDt(atStartOfDay(dateTimeNow));
            timerService.save(timer);
        }
    }
}
