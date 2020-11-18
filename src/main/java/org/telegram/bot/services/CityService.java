package org.telegram.bot.services;

import org.telegram.bot.domain.entities.City;
import org.telegram.bot.domain.entities.User;

import java.util.List;

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
     * Get a City.
     *
     * @param cityName name of City to get.
     * @return the persisted entity.
     */
    City get(String cityName);

    /**
     * Get list of all Cities.
     *
     * @return list of persisted entities.
     */
    List<City> getAll();

    /**
     * Get list of all Cities by User.
     * @param user User entity which is owner of added City
     * @return list of persisted entities.
     */
    List<City> getAll(User user);

    /**
     * Save a City.
     *
     * @param city the entity to save.
     * @return the persisted entity.
     */
    City save(City city);

    /**
     * Remove a City.
     *
     * @param cityId id of City entity
     */
    void remove(Long cityId);

    /**
     * Remove a City.
     *
     * @param city persisted entity for delete
     */
    void remove(City city);
}
