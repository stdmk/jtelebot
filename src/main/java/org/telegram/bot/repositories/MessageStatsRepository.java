package org.telegram.bot.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
    void deleteAllByMessageDateTimeGreaterThanEqual(LocalDateTime dateTime);
}

