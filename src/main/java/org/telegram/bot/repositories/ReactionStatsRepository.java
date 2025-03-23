package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ReactionStats;
import org.telegram.bot.domain.entities.User;

import java.util.List;
import java.util.Set;

public interface ReactionStatsRepository extends JpaRepository<ReactionStats, Long> {
    List<ReactionStats> findByChat(Chat chat);
    List<ReactionStats> findByChatAndUser(Chat chat, User user);
    List<ReactionStats> findByChatAndUserAndEmojiIn(Chat chat, User user, Set<String> emojis);
}
