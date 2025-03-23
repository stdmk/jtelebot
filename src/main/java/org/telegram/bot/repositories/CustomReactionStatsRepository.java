package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CustomReactionStats;
import org.telegram.bot.domain.entities.User;

import java.util.List;
import java.util.Set;

public interface CustomReactionStatsRepository extends JpaRepository<CustomReactionStats, Long> {
    List<CustomReactionStats> findByChat(Chat chat);
    List<CustomReactionStats> findByChatAndUser(Chat chat, User user);
    List<CustomReactionStats> findByChatAndUserAndEmojiIdIn(Chat chat, User user, Set<String> ids);
}
