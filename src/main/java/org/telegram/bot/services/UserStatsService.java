package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.UserStats}.
 */

public interface UserStatsService {

    /**
     * Get UserStats by Chat and User.
     * @param chat - Chat entity.
     * @param user - User entity.
     * @return the persisted entity.
     */
    UserStats get(Chat chat, User user);

    /**
     * Get all group UserStats.
     * @return the persisted entities.
     */
    List<UserStats> getAllGroupStats();

    /**
     * Save the UserStats.
     * @param userStats - entity to save.
     * @return the persisted entity.
     */
    UserStats save(UserStats userStats);

    /**
     * Check and save updates for User, Chat and UserStats etc entites.
     *
     * @param message - received Message.
     */
    void updateEntitiesInfo(Message message, boolean editedMessage);

    /**
     * Get list of users of chat.
     *
     * @param chat - Chat entity.
     * @return list of users of chat
     */
    List<UserStats> getStatsByChat(Chat chat);

    /**
     * Clear user stats by last Month.
     *
     * @return list of tops for send its to chats
     */
    List<SendMessage> clearMonthlyStats();

    /**
     * Increment the user statistics of using commands.
     * @param chat Chat where is User
     * @param user User whose stats will be incremented
     */
    void incrementUserStatsCommands(Chat chat, User user);
}
