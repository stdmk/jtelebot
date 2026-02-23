package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.config.ConditionalOnPropertyNotEmpty;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.TimerService;

import java.time.LocalDateTime;

import static org.telegram.bot.utils.DateUtils.atStartOfDay;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnPropertyNotEmpty("wolframAlphaToken")
public class WolframRequestsTimer implements Timer {

    private final TimerService timerService;
    private final BotStats botStats;

    @Override
    @Scheduled(fixedRate = 86400000)
    public void execute() {
        org.telegram.bot.domain.entities.Timer timer = timerService.get("wolframRequestsTimer");
        LocalDateTime dateTimeNow = LocalDateTime.now();
        if (timer == null) {
            timer = new org.telegram.bot.domain.entities.Timer()
                    .setName("wolframRequestsTimer")
                    .setLastAlarmDt(dateTimeNow);
            timerService.save(timer);
        }

        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusMonths(1).withDayOfMonth(1);

        if (dateTimeNow.isAfter(nextAlarm)) {
            botStats.resetWolframRequests();

            timer.setLastAlarmDt(atStartOfDay(dateTimeNow));
            timerService.save(timer);
        }
    }
}
