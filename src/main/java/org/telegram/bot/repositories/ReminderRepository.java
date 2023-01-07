package org.telegram.bot.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Reminder;
import org.telegram.bot.domain.entities.User;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    Reminder findByChatAndUserAndId(Chat chat, User user, Long reminderId);
    Page<Reminder> findAllByChatAndUser(Chat chat, User user, Pageable pageable);
    List<Reminder> findAllByDateBetweenAndNotified(LocalDate before, LocalDate after, boolean notified);
}
