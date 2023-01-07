package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.City;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;

import javax.annotation.Nullable;
import java.time.ZoneId;
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
     * @param chat Chat entity of UserCity to get.
     * @return the persisted entity.
     */
    UserCity get(User user, Chat chat);

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

    /**
     * @param chat Chat entity of UserCity to get.
     * @param user User entity of UserCity to get.
     * @return zoneId of User.
     */
    @Nullable
    ZoneId getZoneIdOfUser(Chat chat, User user);
}
