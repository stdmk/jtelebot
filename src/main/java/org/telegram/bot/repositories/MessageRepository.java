package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Message;

import java.time.LocalDateTime;

public interface MessageRepository extends JpaRepository<Message, Integer> {
    void deleteAllByDateTimeGreaterThanEqual(LocalDateTime dateTime);
}
