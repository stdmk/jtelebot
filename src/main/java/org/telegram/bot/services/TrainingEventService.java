package org.telegram.bot.services;

import org.telegram.bot.domain.entities.TrainingEvent;
import org.telegram.bot.domain.entities.User;

import java.time.LocalDate;
import java.util.List;

public interface TrainingEventService {
    TrainingEvent get(User user, Long eventId);
    List<TrainingEvent> getAll(LocalDate localDate);
    TrainingEvent save(TrainingEvent trainingEvent);
}
