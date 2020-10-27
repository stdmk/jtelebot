package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Todo;
import org.telegram.bot.domain.entities.User;

import java.util.List;

/**
 * Spring Data repository for the Todo entity.
 */
public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByUser(User user);
}
