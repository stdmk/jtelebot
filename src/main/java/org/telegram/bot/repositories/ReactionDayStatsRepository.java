package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ReactionDayStats;
import org.telegram.bot.domain.entities.User;

import java.util.List;
import java.util.Set;

public interface ReactionDayStatsRepository extends JpaRepository<ReactionDayStats, Long> {
    List<ReactionDayStats> findByChat(Chat chat);
    List<ReactionDayStats> findByChatAndUser(Chat chat, User user);
    List<ReactionDayStats> findByChatAndUserAndEmojiIn(Chat chat, User user, Set<String> emojis);
}
