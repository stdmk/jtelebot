package org.telegram.bot.services;

import org.telegram.bot.domain.entities.CommandWaiting;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.CommandWaiting}.
 */
public interface CommandWaitingService {

    /**
     * Get a CommandWaiting.
     *
     * @param chatId id of Chat entity.
     * @param userId id of User entity.
     * @return the persisted entity.
     */
    CommandWaiting get(Long chatId, Integer userId);

    /**
     * Save a CommandWaiting.
     *
     * @param commandWaiting the entity to save.
     * @return the persisted entity.
     */
    CommandWaiting save(CommandWaiting commandWaiting);

    /**
     * Remove a CommandWaiting.
     *
     * @param commandWaiting the entity to remove.
     */
    void remove(CommandWaiting commandWaiting);
}
