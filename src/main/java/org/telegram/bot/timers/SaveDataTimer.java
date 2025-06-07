package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.services.BotStats;

@Component
@RequiredArgsConstructor
@Slf4j
public class SaveDataTimer extends TimerParent {
    private final BotStats botStats;

    @Override
    @Scheduled(fixedRate = 300000)
    public void execute() {
        botStats.saveStats();
    }
}
