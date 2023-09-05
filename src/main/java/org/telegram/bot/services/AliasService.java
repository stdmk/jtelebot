package org.telegram.bot.services;

import org.springframework.data.domain.Page;
import org.telegram.bot.domain.entities.Alias;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;

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
     * Get Aliases for User and Chat.
     * @param chat Chat entity of Alias to get.
     * @param user User entity of Alias to get.
     * @return the persisted entities.
     */
    List<Alias> getByChatAndUser(Chat chat, User user);

    /**
     * Get Aliases for Chat.
     * @param chat Chat entity of Alias to get.
     * @param user User entity of Alias to get.
     * @param page number of page.
     * @return the persisted entity.
     */
    Page<Alias> getByChatAndUser(Chat chat, User user, int page);

    /**
     * Get Aliases for Chat excluding User.
     * @param chat Chat entity of Alias to get.
     * @param page number of page.
     * @return the persisted entity.
     */
    Page<Alias> getByChat(Chat chat, int page);

    /**
     * Save an Alias.
     *
     * @param alias the entity to save.
     */
    void save(Alias alias);

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
