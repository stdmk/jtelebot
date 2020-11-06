package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.CommandWaiting;

import java.time.LocalDateTime;

/**
 * Spring Data repository for the CommandWaiting entity.
 */
public interface CommandWaitingRepository extends JpaRepository<CommandWaiting, Long> {
    CommandWaiting findByChatIdAndUserId(Long chatId, Integer userId);
}
