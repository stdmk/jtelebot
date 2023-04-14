package org.telegram.bot.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;

import java.time.LocalDateTime;
import java.util.List;

public interface UserStatsRepository extends JpaRepository<UserStats, Long> {
    UserStats findByChatAndUser(Chat chat, User user);
    List<UserStats> findByChat(Chat chat);
    List<UserStats> findByChat(Chat chat, Pageable pageable);
    List<UserStats> findByChatAndLastMessageDateGreaterThan(Chat chat, LocalDateTime dateTime);
    List<UserStats> findByChatAndNumberOfKarmaNot(Chat chat, Pageable pageable, int karma);
    List<UserStats> findByChatAndNumberOfAllKarmaNot(Chat chat, Pageable pageable, long karma);
    List<UserStats> findByChatChatIdLessThan(Long groupAttribute);
}
