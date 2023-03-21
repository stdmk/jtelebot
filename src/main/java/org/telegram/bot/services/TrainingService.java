package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Training;
import org.telegram.bot.domain.entities.User;

import java.time.LocalTime;
import java.util.List;

public interface TrainingService {
    List<Training> get(User user);
    Training get(User user, Long trainingId);
    Training get(User user, LocalTime time, String name);
    void save(Training training);
    void remove(Training training);
}
