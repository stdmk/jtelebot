package org.telegram.bot.services;

import org.telegram.bot.domain.commands.Boobs;
import org.telegram.bot.domain.entities.Token;

/**
 * Service Interface for managing properties.
 */

public interface PropertiesService {
    /**
     * Save a property.
     *
     * @param name of property to save.
     * @param value of property to save
     */
    Boolean save(String name, String value);

    /**
     * Get a property.
     *
     * @param name name of property to get.
     * @return value of property.
     */
    String get(String name);
}
