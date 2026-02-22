package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.config.ConditionalOnPropertyNotEmpty;
import org.telegram.bot.domain.entities.Timer;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.TimerService;

import java.time.LocalDateTime;

import static org.telegram.bot.utils.DateUtils.atStartOfDay;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnPropertyNotEmpty("googleToken")
public class GoogleRequestsTimer extends TimerParent {

    private final TimerService timerService;
    private final BotStats botStats;

    @Override
    @Scheduled(fixedRate = 86400000)
    public void execute() {
        Timer timer = timerService.get("googleRequestsTimer");
        LocalDateTime dateTimeNow = LocalDateTime.now();
        if (timer == null) {
            timer = new Timer()
                    .setName("googleRequestsTimer")
                    .setLastAlarmDt(dateTimeNow);
            timerService.save(timer);
        }

        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusMonths(1).withDayOfMonth(1);

        if (dateTimeNow.isAfter(nextAlarm)) {
            botStats.resetGoogleRequests();

            timer.setLastAlarmDt(atStartOfDay(dateTimeNow));
            timerService.save(timer);
        }
    }
}
