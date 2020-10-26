package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.User}.
 */

public interface ChatService {

    /**
     * Get a Chat.
     *
     * @param chatId of Chat to get.
     * @return the persisted entity.
     */
    Chat get(Long chatId);

    /**
     * Get all chat-groups.
     *
     * @return the persisted entities.
     */
    List<Chat> getAllGroups();

    /**
     * Save a Chat.
     *
     * @param chat the entity to save.
     * @return the persisted entity.
     */
    Chat save(Chat chat);

    /**
     * Get the access level of Chat.
     *
     * @param chatId of entity.
     * @return the persisted entity.
     */
    Integer getChatAccessLevel(Long chatId);
}
