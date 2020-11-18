package org.telegram.bot.services;

import org.telegram.bot.domain.entities.City;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.UserCity}.
 */
public interface UserCityService {
    /**
     * Get a UserCity.
     *
     * @param userCityId of UserCity to get.
     * @return the persisted entity.
     */
    UserCity get(Long userCityId);

    /**
     * Get a UserCity.
     *
     * @param user User entity of UserCity to get.
     * @param chatId id of Chat entity of UserCity to get.
     * @return the persisted entity.
     */
    UserCity get(User user, Long chatId);

    /**
     * Get UserCity list by City.
     *
     * @param city City entity of UserCity to get.
     * @return the persisted entities.
     */
    List<UserCity> getAll(City city);

    /**
     * Save a UserCity.
     *
     * @param userCity the entity to save.
     * @return the persisted entity.
     */
    UserCity save(UserCity userCity);
}
