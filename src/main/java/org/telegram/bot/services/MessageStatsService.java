package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.MessageStats;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.MessageStats}.
 */
public interface MessageStatsService {
    void incrementReplies(int messageId);
    void incrementReactions(int messageId, int reactionsCount);
    List<MessageStats> getByRepliesCountTop(Chat chat, LocalDate date);
    List<MessageStats> getByReactionsCountTop(Chat chat, LocalDate date);
    void removeAll(LocalDateTime expirationDateTime);
}
