package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.TrainingStopped;
import org.telegram.bot.domain.entities.User;

public interface TrainingStoppedRepository extends JpaRepository<TrainingStopped, Long> {
    boolean existsByUser(User user);
    void deleteByUser(User user);
}
