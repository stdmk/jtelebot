package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CustomReactionDayStats;
import org.telegram.bot.domain.entities.User;

import java.util.List;
import java.util.Set;

public interface CustomReactionDayStatsRepository extends JpaRepository<CustomReactionDayStats, Long> {
    List<CustomReactionDayStats> findByChat(Chat chat);
    List<CustomReactionDayStats> findByChatAndUser(Chat chat, User user);
    List<CustomReactionDayStats> findByChatAndUserAndEmojiIdIn(Chat chat, User user, Set<String> ids);
}
