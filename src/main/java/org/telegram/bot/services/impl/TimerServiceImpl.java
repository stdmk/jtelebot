package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Timer;
import org.telegram.bot.repositories.TimerRepository;
import org.telegram.bot.services.TimerService;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimerServiceImpl implements TimerService {

    private final TimerRepository timerRepository;

    @Override
    public Timer get(String name) {
        log.debug("Request to get Timer by name: {} ", name);
        return timerRepository.findByName(name);
    }

    @Override
    public void save(Timer timer) {
        log.debug("Request to save Timer {} ", timer);
        timerRepository.save(timer);
    }
}
