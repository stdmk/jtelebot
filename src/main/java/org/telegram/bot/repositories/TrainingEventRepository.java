package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.TrainSubscription;
import org.telegram.bot.domain.entities.TrainingEvent;
import org.telegram.bot.domain.entities.User;

import java.time.LocalDateTime;
import java.util.List;

public interface TrainingEventRepository extends JpaRepository<TrainingEvent, Long> {
    TrainingEvent findByUserAndIdOrderByTrainingTimeStart(User user, Long id);
    List<TrainingEvent> findByUserOrderByDateTime(User user);
    List<TrainingEvent> findByUserAndDateTimeBetweenOrderByDateTime(User user, LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd);
    List<TrainingEvent> findByUserAndTrainSubscriptionOrderByDateTime(User user, TrainSubscription trainSubscription);
    List<TrainingEvent> findByDateTimeBetweenOrderByTrainingTimeStart(LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd);
    List<TrainingEvent> findByUserAndUnplannedAndDateTimeBetweenOrderByTrainingTimeStart(User user, boolean unplanned, LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd);
    List<TrainingEvent> findByUserAndCanceledAndDateTimeBetweenOrderByTrainingTimeStart(User user, boolean canceled, LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd);
}
