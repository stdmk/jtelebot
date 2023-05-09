package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Timer;
import org.telegram.bot.services.TimerService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;

import static org.telegram.bot.utils.DateUtils.atStartOfDay;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserStatsCleanerTimerTask extends TimerParent {

    private final Bot bot;
    private final TimerService timerService;
    private final UserStatsService userStatsService;

    @Override
    @Scheduled(fixedRate = 10800000)
    public void execute() {
        checkMonthlyStats();
        checkDailyStats();
    }

    private void checkDailyStats() {
        Timer timer = timerService.get("statsDailyCleanTimer");
        if (timer == null) {
            timer = new Timer()
                    .setName("statsDailyCleanTimer")
                    .setLastAlarmDt(LocalDateTime.now());
            timerService.save(timer);
        }

        LocalDateTime dateTimeNow = LocalDateTime.now();
        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusDays(1);

        if (dateTimeNow.isAfter(nextAlarm)) {
            log.info("Timer for cleaning top by day");
            userStatsService.clearDailyStats();

            timer.setLastAlarmDt(atStartOfDay(dateTimeNow));
            timerService.save(timer);
        }
    }

    private void checkMonthlyStats() {
        Timer timer = timerService.get("statsCleanTimer");
        if (timer == null) {
            timer = new Timer()
                    .setName("statsCleanTimer")
                    .setLastAlarmDt(atStartOfDay(LocalDateTime.now().plusMonths(1).withDayOfMonth(1)));
            timerService.save(timer);
        }

        LocalDateTime dateTimeNow = LocalDateTime.now();
        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusMonths(1).withDayOfMonth(1);

        if (dateTimeNow.isAfter(nextAlarm)) {
            log.info("Timer for cleaning top by month");
            userStatsService.clearMonthlyStats().forEach(sendMessage -> {
                try {
                    bot.execute((sendMessage));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            });

            timer.setLastAlarmDt(nextAlarm.withDayOfMonth(1));
            timerService.save(timer);
        }
    }
}
