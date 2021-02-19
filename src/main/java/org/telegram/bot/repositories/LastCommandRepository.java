package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.LastCommand;

/**
 * Spring Data repository for the LastCommand entity.
 */
@Repository
public interface LastCommandRepository extends JpaRepository<LastCommand, Long> {
    LastCommand findByChat(Chat chat);
}
