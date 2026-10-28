package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Message;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.Message}.
 */
public interface MessageService {
    Message get(int messageId);
    List<Message> get(Chat chat, LocalDateTime start, LocalDateTime end);
    void save(org.telegram.bot.domain.model.request.Message message);
    void removeAll(LocalDateTime expirationDateTime);
}
