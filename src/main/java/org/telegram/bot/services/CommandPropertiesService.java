package org.telegram.bot.services;

import org.springframework.data.domain.Page;
import org.telegram.bot.domain.entities.CommandProperties;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.CommandProperties}.
 */

public interface CommandPropertiesService {

    /**
     * Find a class name for command in text.
     *
     * @param textOfMessage - received text of message.
     * @return name of class for process command.
     */
    CommandProperties findCommandInText(String textOfMessage, String botUsername);

    /**
     * Find available commands for level.
     *
     * @param accessLevel - access level for commands.
     * @return list of entities.
     */
    List<CommandProperties> getAvailableCommandsForLevel(Integer accessLevel);

    /**
     * Find the command by its name.
     *
     * @param name - command name (any).
     * @return entity.
     */
    CommandProperties getCommand(String name);

    /**
     * Get the command by its id.
     *
     * @param id - id of entity.
     * @return entity.
     */
    CommandProperties getCommand(Long id);

    /**
     * Get the command name by class.
     *
     * @param commandClass - class of command.
     * @return entity.
     */
    CommandProperties getCommand(Class<?> commandClass);

    /**
     * Get all CommandProperties.
     *
     * @param page page of entities.
     * @return the persisted entities.
     */
    Page<CommandProperties> getAll(int page);

    /**
     * Get all disabled by default CommandProperties.
     *
     * @return the persisted entities.
     */
    List<CommandProperties> getAllDisabledByDefaultForGroups();

    void save(CommandProperties commandProperties);

}
