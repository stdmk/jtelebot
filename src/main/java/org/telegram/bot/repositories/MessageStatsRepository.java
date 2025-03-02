package org.telegram.bot.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.MessageStats;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageStatsRepository extends JpaRepository<MessageStats, Long> {

    MessageStats findByMessageMessageId(int messageId);
    List<MessageStats> findByMessageChatAndRepliesGreaterThanEqualOrderByReactionsDesc(Chat chat, int repliesCount, Pageable pageable);
    List<MessageStats> findByMessageChatAndReactionsGreaterThanEqualOrderByRepliesDesc(Chat chat, int reactionsCount, Pageable pageable);
    void deleteAllByMessageDateTimeGreaterThanEqual(LocalDateTime dateTime);
}

