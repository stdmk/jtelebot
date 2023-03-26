package org.telegram.bot.services;

import org.telegram.bot.domain.entities.TrainSubscription;
import org.telegram.bot.domain.entities.TrainingEvent;
import org.telegram.bot.domain.entities.User;

import java.time.LocalDate;
import java.util.List;

public interface TrainingEventService {
    TrainingEvent get(User user, Long eventId);
    List<TrainingEvent> getAll(LocalDate localDate);
    List<TrainingEvent> getAll(User user);
    List<TrainingEvent> getAllOfYear(User user, int year);
    List<TrainingEvent> getAllOfMonth(User user, int month);
    List<TrainingEvent> getAll(User user, TrainSubscription trainSubscription);
    List<TrainingEvent> getAllUnplanned(User user, LocalDate localDate);
    List<TrainingEvent> getAllCanceled(User user, LocalDate localDate);
    TrainingEvent save(TrainingEvent trainingEvent);
}
