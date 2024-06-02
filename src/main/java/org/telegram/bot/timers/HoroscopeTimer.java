package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.commands.Horoscope;

@Component
@RequiredArgsConstructor
@Slf4j
public class HoroscopeTimer extends TimerParent  {

    private final Horoscope horoscope;

    @Override
    @Scheduled(cron = "0 5 0 * * ?")
    public void execute() {
        horoscope.updateData();
    }
}
