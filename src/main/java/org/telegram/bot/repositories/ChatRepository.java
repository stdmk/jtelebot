package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    Chat findByChatId(Long chatId);
}
