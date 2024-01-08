package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.TodoTag;
import org.telegram.bot.domain.entities.User;

import java.util.List;

public interface TodoTagService {

    /** Get Tag list by values.
     * @param chat Chat entity.
     * @param user User entity.
     * @param tags list of searched tag values.
     * @return search TodoTags.
     */
    List<TodoTag> get(Chat chat, User user, List<String> tags);
}
