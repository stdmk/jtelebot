package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Message;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {
    void deleteAllByDateTimeLessThanEqual(LocalDateTime dateTime);
    List<Message> findByChatAndDateTimeGreaterThanEqualAndDateTimeLessThanOrderByDateTimeAsc(
            Chat chat,
            LocalDateTime start,
            LocalDateTime end);
}
