package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Message;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {
    @Modifying
    @Query(value = "DELETE FROM bot.message " +
            "WHERE datetime <= :dateTime " +
            "OR chatid NOT IN (SELECT chatid FROM bot.chat) " +
            "OR userid NOT IN (SELECT userid FROM bot.user)", nativeQuery = true)
    void deleteExpiredAndOrphaned(@Param("dateTime") LocalDateTime dateTime);

    List<Message> findByChatAndDateTimeGreaterThanEqualAndDateTimeLessThanOrderByDateTimeAsc(
            Chat chat,
            LocalDateTime start,
            LocalDateTime end);
}
