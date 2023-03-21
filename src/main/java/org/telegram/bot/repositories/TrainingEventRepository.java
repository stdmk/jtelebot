package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.TrainingEvent;
import org.telegram.bot.domain.entities.User;

import java.time.LocalDateTime;
import java.util.List;

public interface TrainingEventRepository extends JpaRepository<TrainingEvent, Long> {
    TrainingEvent findByUserAndId(User user, Long id);
    List<TrainingEvent> findByDateTimeBetween(LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd);
}
