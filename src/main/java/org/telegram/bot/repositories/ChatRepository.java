package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.telegram.bot.domain.entities.Chat;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    Chat findByChatId(Long chatId);

    List<Chat> findByChatIdLessThan(Long groupAttribute);

    @Query("SELECT DISTINCT h.chat FROM Holiday h")
    List<Chat> findDistinctChatWithHolidays();
}
