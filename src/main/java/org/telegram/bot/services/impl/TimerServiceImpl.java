package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Timer;
import org.telegram.bot.repositories.TimerRepository;
import org.telegram.bot.services.TimerService;

@Service
@AllArgsConstructor
public class TimerServiceImpl implements TimerService {

    private final Logger log = LoggerFactory.getLogger(TimerServiceImpl.class);

    private final TimerRepository timerRepository;

    @Override
    public Timer get(String name) {
        log.debug("Request to get Timer by name: {} ", name);
        return timerRepository.findByName(name);
    }

    @Override
    public Timer save(Timer timer) {
        log.debug("Request to save Timer {} ", timer);
        return timerRepository.save(timer);
    }
}
