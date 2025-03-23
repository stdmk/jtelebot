package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Increment;
import org.telegram.bot.domain.entities.User;

import java.util.List;

public interface IncrementRepository extends JpaRepository<Increment, Long> {
    Increment findByChatAndUserAndName(Chat chat, User user, String name);
    List<Increment> findByChatAndUser(Chat chat, User user);
}
