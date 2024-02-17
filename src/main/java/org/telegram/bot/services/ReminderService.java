package org.telegram.bot.services;

import org.springframework.data.domain.Page;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Reminder;
import org.telegram.bot.domain.entities.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.Reminder}.
 */
public interface ReminderService {

    /**
     * Get a Reminder.
     *
     * @param user User owner of Reminder.
     * @param reminderId of Reminder to get.
     * @return the persisted entity.
     */
    Reminder get(Chat chat, User user, Long reminderId);

    /**
     * Get a Reminder.
     *
     * @param reminderId of Reminder to get.
     * @return the persisted entity.
     */
    Reminder get(Long reminderId);

    /**
     * Get all not notified Reminders by date.
     *
     * @param date for filter.
     * @return the persisted entities.
     */
    List<Reminder> getAllNotNotifiedByDate(LocalDate date);

    /**
     * Get all not notified Reminders before date.
     *
     * @param date for filter.
     * @return the persisted entities.
     */
    List<Reminder> getAllNotNotifiedBeforeDate(LocalDate date);

    /**
     * Get Reminders for Chat and User.
     * @param chat Chat entity of Reminder to get.
     * @param user User entity of Reminder to get.
     * @param page number of page.
     * @return persisted entities.
     */
    Page<Reminder> getByChatAndUser(Chat chat, User user, int page);

    /**
     * Save a Reminder.
     *
     * @param reminder the entity to save.
     * @return the persisted entity.
     */
    Reminder save(Reminder reminder);

    /**
     * Remove a Reminder.
     *
     * @param reminder the entity to delete.
     */
    void remove(Reminder reminder);

    /**
     * Get next alarm date time for Reminder.
     *
     * @param reminder the Reminder entity.
     * @return date time of next planned alarm.
     */
    LocalDateTime getNextAlarmDateTime(Reminder reminder);
}
