package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Todo;
import org.telegram.bot.domain.entities.User;

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
     * Get a Todo list.
     *
     * @param chat Chat entity.
     * @param user User entity.
     * @return the persisted entity.
     */
    List<Todo> get(Chat chat, User user);

    /**
     * Get a Todo list.
     *
     * @param chat Chat entity.
     * @param todoId of Todo to get.
     * @return the persisted entity.
     */
    Todo get(Chat chat, Long todoId);

    /**
     * Save a Todo.
     *
     * @param todo the entity to save.
     */
    void save(Todo todo);

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
    boolean remove(Long todoId);
}
