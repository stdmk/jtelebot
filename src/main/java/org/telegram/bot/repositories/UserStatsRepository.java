package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.UserStats;

import java.util.List;

public interface UserStatsRepository extends JpaRepository<UserStats, Long> {
    UserStats findByChatIdAndUserUserId(Long chatId, Integer userId);
    List<UserStats> findByChatId(Long chatId);
    List<UserStats> findByChatIdLessThan(Long groupAttribute);
}
