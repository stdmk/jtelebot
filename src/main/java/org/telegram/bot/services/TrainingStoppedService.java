package org.telegram.bot.services;

import org.telegram.bot.domain.entities.TrainingStopped;
import org.telegram.bot.domain.entities.User;

import java.util.List;

public interface TrainingStoppedService {
    boolean isStopped(User user);
    void stop(User user);
    void start(User user);
    List<TrainingStopped> getAll();
}
