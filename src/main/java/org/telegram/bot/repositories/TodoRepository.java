package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Todo;

/**
 * Spring Data repository for the Todo entity.
 */
public interface TodoRepository extends JpaRepository<Todo, Long> {
}
