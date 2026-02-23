package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.telegram.bot.services.BotStats;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TimerErrorAspect {

    private final BotStats botStats;

    @AfterThrowing(pointcut = "execution(* org.telegram.bot.timers.Timer.execute(..))", throwing = "e")
    public void handleSchedulerException(Exception e) {
        String errorMessage = "Error in timer: " + e.getMessage();
        log.error(errorMessage, e);
        botStats.incrementErrors(e, errorMessage);
    }

}
