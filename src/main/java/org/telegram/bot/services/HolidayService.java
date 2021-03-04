package org.telegram.bot.services;

import org.springframework.data.domain.Page;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Holiday;
import org.telegram.bot.domain.entities.User;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.Holiday}.
 */
public interface HolidayService {
    /**
     * Get a Holiday.
     *
     * @param holidayId of UserTv to get.
     * @return the persisted entity.
     */
    Holiday get(Long holidayId);

    /**
     * Get a Holiday.
     *
     * @param chat Chat entity of UserCity to get.
     * @param user User entity of UserCity to get.
     * @param name of entity of Holiday to get.
     * @return the persisted entity.
     */
    Holiday get(Chat chat, User user, String name);

    /**
     * Searching fo holidays.
     *
     * @param chat Chat entity of UserCity to get.
     * @param name of entity of Holiday to get.
     * @return persisted entities.
     */
    List<Holiday> get(Chat chat, String name);

    /**
     * Get a Holidays.
     *
     * @param chat Chat entity of UserCity to get.
     * @param user User entity of UserCity to get.
     * @return the persisted entities.
     */
    List<Holiday> get(Chat chat, User user);

    /**
     * Get a Holidays.
     *
     * @param chat Chat entity of UserCity to get.
     * @return the persisted entities.
     */
    List<Holiday> get(Chat chat);

    /**
     * Get a Holidays.
     *
     * @param chat Chat entity of UserCity to get.
     * @param user User entity of UserCity to get.
     * @return the persisted entities.
     */
    Page<Holiday> get(Chat chat, User user, int page);

    /**
     * Save a Holiday.
     *
     * @param holiday the entity to save.
     * @return the persisted entity.
     */
    Holiday save(Holiday holiday);

    /**
     * Remove a Holiday.
     *
     * @param holiday persisted entity for delete
     */
    void remove(Holiday holiday);
}
