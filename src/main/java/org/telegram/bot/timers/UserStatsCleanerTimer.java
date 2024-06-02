package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.Bot;
import org.telegram.bot.commands.Top;
import org.telegram.bot.domain.entities.Timer;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.TimerService;
import org.telegram.bot.services.UserStatsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.atStartOfDay;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserStatsCleanerTimer extends TimerParent {

    private final Bot bot;
    private final TimerService timerService;
    private final UserStatsService userStatsService;
    private final ChatService chatService;
    private final Top top;

    @Autowired
    @Lazy
    private UserStatsCleanerTimer self;

    @Override
    @Scheduled(fixedRate = 10800000)
    public void execute() {
        self.checkMonthlyStats();
        self.checkDailyStats();
    }

    @Transactional
    public void checkDailyStats() {
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

    @Transactional
    public void checkMonthlyStats() {
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

            List<TextResponse> sendMessageListWithMonthlyTop = chatService.getAllGroups()
                    .stream()
                    .map(top::getTopByChat)
                    .collect(Collectors.toList());

            userStatsService.clearMonthlyStats();

            sendMessageListWithMonthlyTop.forEach(bot::sendMessage);

            timer.setLastAlarmDt(nextAlarm.withDayOfMonth(1));
            timerService.save(timer);
        }
    }
}
