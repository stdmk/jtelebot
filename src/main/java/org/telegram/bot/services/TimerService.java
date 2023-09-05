package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Timer;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.Timer}.
 */

public interface TimerService {
    /**
     * Get a Timer by it's name.
     *
     * @param name of Timer to get.
     * @return the persisted entity.
     */
    Timer get(String name);

    /**
     * Save a Timer.
     *
     * @param timer the entity to save.
     */
    void save(Timer timer);
}
