package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.providers.sber.SberTokenProvider;

@Component
@RequiredArgsConstructor
@Slf4j
public class SberTokenUpdateTimer extends TimerParent {

    private final SberTokenProvider sberTokenProvider;

    @Override
    @Scheduled(fixedRate = 5000)
    public void execute() {
        sberTokenProvider.updateTokens();
    }
}
