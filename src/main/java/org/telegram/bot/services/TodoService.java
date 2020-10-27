package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Todo;

import java.util.List;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.Todo}.
 */
public interface TodoService {

    /**
     * Get a Todo.
     *
     * @param todoId of Todo to get.
     * @return the persisted entity.
     */
    Todo get(Long todoId);

    /**
     * Save a Todo.
     *
     * @param todo the entity to save.
     * @return the persisted entity.
     */
    Todo save(Todo todo);

    /**
     * Get a Todo list.
     *
     * @return list of persisted entities.
     */
    List<Todo> getList();

    /**
     * Remove a Todo.
     *
     * @param todoId of Todo to remove.
     * @return true if remove.
     */
    Boolean remove(Long todoId);
}
