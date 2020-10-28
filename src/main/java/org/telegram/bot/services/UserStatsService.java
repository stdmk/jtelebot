package org.telegram.bot.services;

import org.telegram.bot.domain.entities.UserStats;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.UserStats}.
 */

public interface UserStatsService {

    /**
     * Get UserStats by Chat and User.
     * @param chatId - current chat id.
     * @param userId - current user id.
     * @return the persisted entity.
     */
    UserStats get(Long chatId, Integer userId);

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
     * @param update - received from api update.
     */
    void updateEntitiesInfo(Update update);

    /**
     * Get list of users of chat.
     *
     * @param chatId - id of chat with users
     * @return list of users of chat
     */
    List<UserStats> getUsersByChatId(Long chatId);

    /**
     * Clear user stats by last Month.
     *
     * @return list of tops for send its to chats
     */
    List<SendMessage> clearMonthlyStats();

    /**
     * Increment the user statistics of using commands.
     * @param chatId id of Chat where is User
     * @param userId id of User whose stats will be incremented
     */
    void incrementUserStatsCommands(Long chatId, Integer userId);
}
