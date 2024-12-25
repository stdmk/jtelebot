package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.DelayCommand;
import org.telegram.bot.repositories.DelayCommandRepository;
import org.telegram.bot.services.DelayCommandService;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class DelayCommandServiceImpl implements DelayCommandService {

    private final DelayCommandRepository delayCommandRepository;

    @Override
    public DelayCommand save(DelayCommand delayCommand) {
        return delayCommandRepository.save(delayCommand);
    }

    @Override
    public List<DelayCommand> getAllBeforeDateTime(LocalDateTime dateTime) {
        return delayCommandRepository.findAllByDateTimeLessThanEqual(dateTime);
    }

    @Override
    public void remove(DelayCommand delayCommand) {
        delayCommandRepository.delete(delayCommand);
    }

}
