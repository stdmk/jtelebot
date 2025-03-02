package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Message;

import java.time.LocalDateTime;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.Message}.
 */
public interface MessageService {
    Message get(int messageId);
    void save(org.telegram.bot.domain.model.request.Message message);
    void removeAll(LocalDateTime expirationDateTime);
}
