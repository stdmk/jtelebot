package org.telegram.bot.repositories;

import org.springframework.data.repository.CrudRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.DisableCommand;

import java.util.List;

public interface DisableCommandRepository extends CrudRepository<DisableCommand, Long> {
    List<DisableCommand> findByChat(Chat chat);
    DisableCommand findByChatAndCommandProperties(Chat chat, CommandProperties commandProperties);
}
