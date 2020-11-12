package org.telegram.bot.services;

import org.telegram.bot.domain.entities.UserCity;

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
     * Save a UserCity.
     *
     * @param userCity the entity to save.
     * @return the persisted entity.
     */
    UserCity save(UserCity userCity);
}
