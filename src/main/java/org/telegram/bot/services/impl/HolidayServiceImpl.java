package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Holiday;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.HolidayRepository;
import org.telegram.bot.services.HolidayService;

import java.util.List;

@Service
@AllArgsConstructor
public class HolidayServiceImpl implements HolidayService {

    private final Logger log = LoggerFactory.getLogger(UserCityServiceImpl.class);

    private final HolidayRepository holidayRepository;

    @Override
    public Holiday get(Long holidayId) {
        log.debug("Request to get Holiday by holidayId: {}", holidayId);
        return holidayRepository.findById(holidayId).orElse(null);
    }

    @Override
    public Holiday get(Chat chat, User user, String name) {
        log.debug("Request to get Holiday by User: {} and Chat: {} and name {}", user, chat, name);
        return holidayRepository.findByChatAndUserAndName(chat, user, name);
    }

    @Override
    public List<Holiday> get(Chat chat, User user) {
        log.debug("Request to get Holiday by User: {} and Chat: {}", user, chat);
        return holidayRepository.findByChatAndUser(chat, user);
    }

    @Override
    public Page<Holiday> get(Chat chat, User user, int page) {
        log.debug("Request to get pageble Holiday by User: {} and Chat: {}", user, chat);
        return holidayRepository.findAll(PageRequest.of(page, 6));
    }

    @Override
    public Holiday save(Holiday holiday) {
        log.debug("Request to save Holiday: {}", holiday);
        return holidayRepository.save(holiday);
    }

    @Override
    public void remove(Holiday holiday) {
        log.debug("Request to delete Holiday: {}", holiday);
        holidayRepository.delete(holiday);
    }
}
