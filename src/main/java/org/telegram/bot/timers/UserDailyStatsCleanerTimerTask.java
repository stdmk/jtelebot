package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.services.UserStatsService;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDailyStatsCleanerTimerTask extends TimerParent {
    private final UserStatsService userStatsService;

    @Override
    @Scheduled(cron = "0 0 0 * * ?")
    public void execute() {
        log.info("Timer for cleaning top by day");
        userStatsService.clearDailyStats();
    }

}
