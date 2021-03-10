package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.LastCommand;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.LastCommand}.
 */
public interface LastCommandService {
    /**
     * Get a LastCommand.
     *
     * @param id of LastCommand to get.
     * @return the persisted entity.
     */
    LastCommand get(Long id);

    /**
     * Get a LastCommand.
     *
     * @param chat Chat entity of Alias to get.
     * @return the persisted entity.
     */
    LastCommand get(Chat chat);

    /**
     * Save a LastCommand.
     *
     * @param lastCommand the entity to save.
     * @return the persisted entity.
     */
    LastCommand save(LastCommand lastCommand);
}
