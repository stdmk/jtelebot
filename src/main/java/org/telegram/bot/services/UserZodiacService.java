package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserZodiac;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.UserZodiac}.
 */
public interface UserZodiacService {

    /**
     * Get a UserZodiac.
     *
     * @param chat Chat entity of UserCity to get.
     * @param user User entity of UserCity to get.
     * @return the persisted entity.
     */
    UserZodiac get(Chat chat, User user);

    /**
     * Save a UserZodiac.
     *
     * @param userZodiac the entity to save.
     * @return the persisted entity.
     */
    UserZodiac save(UserZodiac userZodiac);
}
