package org.telegram.bot.mapper.telegram.request;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.User;

@Component
public class UserMapper {

    public User toUser(org.telegram.telegrambots.meta.api.objects.User user) {
        String username = user.getUserName();
        if (username == null) {
            username = user.getFirstName();
        }

        return new User()
                .setUserId(user.getId())
                .setUsername(username)
                .setLang(user.getLanguageCode());
    }

}
