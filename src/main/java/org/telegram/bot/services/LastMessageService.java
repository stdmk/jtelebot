package org.telegram.bot.services;

import org.telegram.bot.domain.entities.LastMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.LocalDateTime;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.LastMessage}.
 */

public interface LastMessageService {
    /**
     * Get a LastMessage.
     *
     * @param id of LastMessage to get.
     * @return the persisted entity.
     */
    LastMessage get(Long id);

    /**
     * Save a LastMessage.
     *
     * @param lastMessage the entity to save.
     * @return the persisted entity.
     */
    LastMessage save(LastMessage lastMessage);

    /**
     * Update a LastMessage.
     *
     * @param lastMessage the entity to update.
     * @param message containing updates
     * @return the persisted entity.
     */
    LastMessage update(LastMessage lastMessage, Message message);
}
