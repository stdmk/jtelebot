package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.LastMessage;

public interface LastMessageRepository extends JpaRepository<LastMessage, Long> {
}
