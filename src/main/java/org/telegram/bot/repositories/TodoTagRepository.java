package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.TodoTag;
import org.telegram.bot.domain.entities.User;

import java.util.List;

public interface TodoTagRepository extends JpaRepository<TodoTag, Long> {
    List<TodoTag> findByChatAndUserAndTagIn(Chat chat, User user, List<String> tags);
}
