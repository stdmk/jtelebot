package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.User;

/**
 * Spring Data repository for the CommandWaiting entity.
 */
public interface CommandWaitingRepository extends JpaRepository<CommandWaiting, Long> {
    CommandWaiting findByChatAndUser(Chat chat, User user);
    void deleteByChatAndUser(Chat chat, User user);
}
