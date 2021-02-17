package org.telegram.bot.services;

import org.telegram.bot.domain.entities.*;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.UserTv}.
 */
public interface UserTvService {
    /**
     * Get a UserTv.
     *
     * @param userTvId of UserTv to get.
     * @return the persisted entity.
     */
    UserTv get(Long userTvId);

    /**
     * Get a UserTv.
     *
     * @param chat Chat entity of UserCity to get.
     * @param user User entity of UserCity to get.
     * @param tvChannel entity of TvChannel to get.
     * @return the persisted entity.
     */
    UserTv get(Chat chat, User user, TvChannel tvChannel);

    /**
     * Get a UserTvs.
     *
     * @param chat Chat entity of UserCity to get.
     * @param user User entity of UserCity to get.
     * @return the persisted entities.
     */
    List<UserTv> get(Chat chat, User user);

    /**
     * Save a UserTv.
     *
     * @param userTv the entity to save.
     * @return the persisted entity.
     */
    UserTv save(UserTv userTv);

    /**
     * Remove a UserTv.
     *
     * @param userTv persisted entity for delete
     */
    void remove(UserTv userTv);
}
