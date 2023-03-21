package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Training;
import org.telegram.bot.domain.entities.TrainingScheduled;
import org.telegram.bot.domain.entities.User;

import java.time.DayOfWeek;
import java.util.List;

public interface TrainingScheduledService {
    TrainingScheduled get(User user, DayOfWeek dayOfWeek, Training training);
    List<TrainingScheduled> get(User user, DayOfWeek dayOfWeek);
    List<TrainingScheduled> get(User user);
    List<TrainingScheduled> getAll(DayOfWeek dayOfWeek);
    void save(TrainingScheduled trainingScheduled);
    void remove(TrainingScheduled trainingScheduled);
    void removeAllByTraining(Training training);
}
