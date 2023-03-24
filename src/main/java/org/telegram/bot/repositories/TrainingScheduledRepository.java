package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Training;
import org.telegram.bot.domain.entities.TrainingScheduled;
import org.telegram.bot.domain.entities.User;

import java.time.DayOfWeek;
import java.util.List;

public interface TrainingScheduledRepository extends JpaRepository<TrainingScheduled, Long> {
    TrainingScheduled findByUserAndDayOfWeekAndTrainingOrderByTrainingTimeStart(User user, DayOfWeek dayOfWeek, Training training);
    List<TrainingScheduled> findByUserAndDayOfWeekOrderByTrainingTimeStart(User user, DayOfWeek dayOfWeek);
    List<TrainingScheduled> findByUserOrderByTrainingTimeStart(User user);
    List<TrainingScheduled> findAllByDayOfWeekOrderByTrainingTimeStart(DayOfWeek dayOfWeek);
    void deleteByTraining(Training training);
}
