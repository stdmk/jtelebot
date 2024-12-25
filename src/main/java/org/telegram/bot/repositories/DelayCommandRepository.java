package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.DelayCommand;

import java.time.LocalDateTime;
import java.util.List;

public interface DelayCommandRepository extends JpaRepository<DelayCommand, Long> {
    List<DelayCommand> findAllByDateTimeLessThanEqual(LocalDateTime before);
}
