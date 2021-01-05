package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;

import java.util.List;

public interface UserStatsRepository extends JpaRepository<UserStats, Long> {
    UserStats findByChatAndUser(Chat chat, User user);
    List<UserStats> findByChat(Chat chat);
    List<UserStats> findByChatChatIdLessThan(Long groupAttribute);
}
