package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Alias;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.exception.BotException;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.Alias}.
 */
public interface AliasService {
    /**
     * Get an Alias.
     *
     * @param aliasId of Alias to get.
     * @return the persisted entity.
     */
    Alias get(Long aliasId);

    /**
     * Get an Alias.
     *
     * @param chat Chat entity of Alias to get.
     * @param user User entity of Alias to get.
     * @param aliasId of Alias to get.
     * @return the persisted entity.
     */
    Alias get(Chat chat, User user, Long aliasId);

    /**
     * Get an Alias.
     *
     * @param chat Chat entity of Alias to get.
     * @param user User entity of Alias to get.
     * @param name name of Alias.
     * @return the persisted entity.
     */
    Alias get(Chat chat, User user, String name);

    /**
     * Get an Alias.
     * @param chat Chat entity of Alias to get.
     * @param user User entity of Alias to get.
     * @return the persisted entity.
     */
    List<Alias> get(Chat chat, User user);

    /**
     * Save an Alias.
     *
     * @param alias the entity to save.
     * @return the persisted entity.
     */
    Alias save(Alias alias);

    /**
     * Delete an Alias.
     * @param chat Chat entity of Alias to get.
     * @param user User entity of Alias to get.
     * @param aliasId of the entity to delete.
     * @return true if deleted.
     */
    Boolean remove(Chat chat, User user, Long aliasId);

    /**
     * Delete an Alias.
     * @param chat Chat entity of Alias to get.
     * @param user User entity of Alias to get.
     * @param name of the alias to delete.
     * @return true if deleted.
     */
    Boolean remove(Chat chat, User user, String name);
}
