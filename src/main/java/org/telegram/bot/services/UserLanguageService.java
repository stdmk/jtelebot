package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserLanguage;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.UserLanguage}.
 */
public interface UserLanguageService {

    /**
     * Save a UserLanguage.
     *
     * @param chat Chat entity.
     * @param user User entity.
     * @param lang language code.
     */
    void save(Chat chat, User user, String lang);

    /**
     * Get a UserLanguage.
     *
     * @param chat Chat entity.
     * @param user User entity.
     * @return stored entity.
     */
    UserLanguage get(Chat chat, User user);
}
