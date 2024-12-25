package org.telegram.bot.services;

import org.telegram.bot.domain.entities.DelayCommand;

import java.time.LocalDateTime;
import java.util.List;

public interface DelayCommandService {
    DelayCommand save(DelayCommand delayCommand);
    List<DelayCommand> getAllBeforeDateTime(LocalDateTime dateTime);
    void remove(DelayCommand delayCommand);
}
