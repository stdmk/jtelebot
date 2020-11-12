package org.telegram.bot.services;

import org.telegram.bot.domain.entities.City;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.City}.
 */
public interface CityService {
    /**
     * Get a City.
     *
     * @param cityId of City to get.
     * @return the persisted entity.
     */
    City get(Long cityId);

    /**
     * Save a City.
     *
     * @param city the entity to save.
     * @return the persisted entity.
     */
    City save(City city);
}
