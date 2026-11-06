package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CustomReactionMonthStats;
import org.telegram.bot.domain.entities.User;

import java.util.List;
import java.util.Set;

public interface CustomReactionMonthStatsRepository extends JpaRepository<CustomReactionMonthStats, Long> {
    @Modifying
    @Query(value = "TRUNCATE TABLE bot.customreactionmonthstats", nativeQuery = true)
    void deleteAllInBulk();

    List<CustomReactionMonthStats> findByChat(Chat chat);
    List<CustomReactionMonthStats> findByChatAndUser(Chat chat, User user);
    List<CustomReactionMonthStats> findByChatAndUserAndEmojiIdIn(Chat chat, User user, Set<String> ids);
}
