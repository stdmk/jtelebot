package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Timer;

public interface TimerRepository extends JpaRepository<Timer, Long> {
    Timer findByName(String name);
}
