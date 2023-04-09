package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Reminder;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.ReminderRepository;
import org.telegram.bot.services.ReminderService;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderServiceImpl implements ReminderService {

    private final ReminderRepository reminderRepository;

    @Override
    public Reminder get(Chat chat, User user, Long reminderId) {
        log.debug("Request to get Reminder of User {} for Chat {} by id: {}", user, chat, reminderId);
        return reminderRepository.findByChatAndUserAndId(chat, user, reminderId);
    }

    @Override
    public List<Reminder> getAllNotNotifiedByDate(LocalDate date) {
        log.debug("Request to get all Reminders by date {}", date);
        return reminderRepository.findAllByDateBetweenAndNotified(
                date.minusDays(1),
                date.plusDays(1),
                false);
    }

    @Override
    public Page<Reminder> getByChatAndUser(Chat chat, User user, int page) {
        log.debug("Request to get reminders by Chat: {}, User: {}, page: {}", chat, user, page);
        return reminderRepository.findAllByChatAndUserOrderByDateAscTimeAsc(chat, user, PageRequest.of(page, 5));
    }

    @Override
    public Reminder save(Reminder reminder) {
        log.debug("Request to save Reminder: {}", reminder);
        return reminderRepository.save(reminder);
    }

    @Override
    public void remove(Reminder reminder) {
        log.debug("Request to remove Reminder: {}", reminder);
        reminderRepository.delete(reminder);
    }
}
