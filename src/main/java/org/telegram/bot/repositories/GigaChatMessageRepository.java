package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.GigaChatMessage;
import org.telegram.bot.domain.entities.User;

import java.util.List;

public interface GigaChatMessageRepository extends JpaRepository<GigaChatMessage, Long> {
    List<GigaChatMessage> findByChat(Chat chat);
    List<GigaChatMessage> findByUserAndChat(User user, Chat chat);
    void deleteAllByChat(Chat chat);
    void deleteAllByUserAndChat(User user, Chat chat);
}
