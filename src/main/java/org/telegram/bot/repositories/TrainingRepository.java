package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Training;
import org.telegram.bot.domain.entities.User;

import java.time.LocalTime;
import java.util.List;

public interface TrainingRepository extends JpaRepository<Training, Long> {
    List<Training> findByUserAndDeletedOrderByTimeStart(User user, boolean deleted);
    Training findByUserAndIdAndDeletedOrderByTimeStart(User user, Long id, boolean deleted);
    Training findByUserAndTimeStartAndNameIgnoreCaseAndDeletedOrderByTimeStart(User user, LocalTime time, String name, boolean deleted);
}
