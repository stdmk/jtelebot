package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Holiday;
import org.telegram.bot.domain.entities.User;

import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    Holiday findByChatAndUserAndName(Chat chat, User user, String name);
    List<Holiday> findByChatAndUser(Chat chat, User user);
}
