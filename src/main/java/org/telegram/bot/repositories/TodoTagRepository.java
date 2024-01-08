package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.TodoTag;

import java.util.List;

public interface TodoTagRepository extends JpaRepository<TodoTag, Long> {
    List<TodoTag> findByChatAndUserAndTagIn(List<String> tags);
}
