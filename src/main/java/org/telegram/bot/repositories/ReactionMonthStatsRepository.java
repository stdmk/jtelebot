package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ReactionMonthStats;
import org.telegram.bot.domain.entities.User;

import java.util.List;
import java.util.Set;

public interface ReactionMonthStatsRepository extends JpaRepository<ReactionMonthStats, Long> {
    List<ReactionMonthStats> findByChat(Chat chat);
    List<ReactionMonthStats> findByChatAndUser(Chat chat, User user);
    List<ReactionMonthStats> findByChatAndUserAndEmojiIn(Chat chat, User user, Set<String> emojis);
}
