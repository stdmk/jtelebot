package org.telegram.bot.timers;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.services.TimerService;

@Component
@AllArgsConstructor
public class NewsTimer extends TimerParent {

    private final Logger log = LoggerFactory.getLogger(NewsTimer.class);

    private final ApplicationContext context;
    private final TimerService timerService;

    @Override
    @Scheduled(fixedRate = 300000)
    public void execute() {

    }
}
