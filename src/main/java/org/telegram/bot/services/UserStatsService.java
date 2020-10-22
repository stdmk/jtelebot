package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.telegrambots.meta.api.objects.Update;

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
}
