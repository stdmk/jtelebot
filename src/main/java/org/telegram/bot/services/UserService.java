package org.telegram.bot.services;

import org.telegram.bot.domain.entities.User;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.User}.
 */

public interface UserService {
    /**
     * Get a User.
     *
     * @param userId of User to get.
     * @return the persisted entity.
     */
    User get(Long userId);

    /**
     * Get a User.
     *
     * @param username of User to get.
     * @return the persisted entity.
     */
    User get(String username);

    /**
     * Save a User.
     *
     * @param user the entity to save.
     * @return the persisted entity.
     */
    User save(User user);

    /**
     * Get the access level of User.
     *
     * @param userId of entity.
     * @return the persisted entity.
     */
    Integer getUserAccessLevel(Long userId);
}
