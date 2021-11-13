package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.DisableCommand;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.DisableCommand}.
 */
public interface DisableCommandService {

    /**
     * Get list of DisableCommand.
     *
     * @param chat Chat entity.
     * @return the persisted entity.
     */
    List<DisableCommand> getByChat(Chat chat);

    /**
     * Get DisableCommand.
     *
     * @param id id of entity.
     * @return the persisted entity.
     */
    DisableCommand get(Long id);

    /**
     * Get a DisableCommand.
     *
     * @param chat Chat entity of Chat to get.
     * @param commandProperties entity of CommandProperties to get.
     * @return the persisted entity.
     */
    DisableCommand get(Chat chat, CommandProperties commandProperties);

    /**
     * Save a DisableCommand.
     *
     * @param disableCommand the entity to save.
     * @return the persisted entity.
     */
    DisableCommand save(DisableCommand disableCommand);

    /**
     * Remove a DisableCommand.
     *
     * @param disableCommand persisted entity for delete
     */
    void remove(DisableCommand disableCommand);
}
