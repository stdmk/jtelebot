package org.telegram.bot.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Message;
import org.telegram.bot.domain.entities.MessageStats;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface MessageStatsRepository extends JpaRepository<MessageStats, Long> {

    MessageStats findByMessage(Message message);
    List<MessageStats> findByMessageChatAndDateAndRepliesGreaterThanEqualOrderByReactionsDesc(Chat chat, LocalDate date, int repliesCount, Pageable pageable);
    List<MessageStats> findByMessageChatAndDateAndReactionsGreaterThanEqualOrderByRepliesDesc(Chat chat, LocalDate date, int reactionsCount, Pageable pageable);
    @Modifying
    @Query(value = "DELETE FROM bot.messagestats " +
            "WHERE messageid IN (SELECT messageid FROM bot.message WHERE datetime <= :dateTime) " +
            "OR NOT EXISTS (SELECT 1 FROM bot.message WHERE bot.message.messageid = bot.messagestats.messageid)",
            nativeQuery = true)
    void deleteExpiredAndOrphaned(@Param("dateTime") LocalDateTime dateTime);
}

