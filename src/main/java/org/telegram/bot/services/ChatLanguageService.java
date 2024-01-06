package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatLanguage;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.ChatLanguage}.
 */
public interface ChatLanguageService {

    /**
     * Save the ChatLanguage.
     *
     * @param chat Chat entity.
     * @param lang code of language.
     */
    void save(Chat chat, String lang);

    /**
     * Get a ChatLanguage.
     *
     * @param chat Chat entity of UserLanguage to get.
     * @return the persisted entity.
     */
    ChatLanguage get(Chat chat);

}
