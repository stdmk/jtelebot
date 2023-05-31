package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatGPTMessage;
import org.telegram.bot.domain.entities.User;

import java.util.List;

public interface ChatGPTMessageRepository extends JpaRepository<ChatGPTMessage, Long> {
    List<ChatGPTMessage> findByChat(Chat chat);
    List<ChatGPTMessage> findByUserAndChat(User user, Chat chat);
    void deleteAllByChat(Chat chat);
    void deleteAllByUserAndChat(User user, Chat chat);
}
