package org.telegram.bot.services;

import org.telegram.bot.domain.entities.User;
import org.telegram.bot.enums.AccessLevel;

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

    /**
     * Get user access level based on userAccessLevel and chatAccessLevel.
     * @param userId id of User entity
     * @param chatId id of Chat entity
     * @return the highest of access levels
     */
    AccessLevel getCurrentAccessLevel(Long userId, Long chatId);

    /**
     * Сhecks if the access level is sufficient for command.
     * @param user User entity
     * @param commandAccessLevel access level to command
     * @return true if sufficient
     */
    boolean isUserHaveAccessForCommand(User user, Integer commandAccessLevel);

    /**
     * Сhecks if the access level is sufficient for command.
     * @param userAccessLevel access level of User
     * @param commandAccessLevel access level to command
     * @return true if sufficient
     */
    boolean isUserHaveAccessForCommand(AccessLevel userAccessLevel, AccessLevel commandAccessLevel);

    /**
     * Сhecks if the access level is sufficient for command.
     * @param userAccessLevel access level of User
     * @param commandAccessLevel access level to command
     * @return true if sufficient
     */
    boolean isUserHaveAccessForCommand(Integer userAccessLevel, Integer commandAccessLevel);

}
