package org.telegram.bot.mapper.telegram.request;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void toUserWithoutUsernameTest() {
        final long userId = 1L;
        final String firstName = "firstName";
        final String lang = "en";

        org.telegram.telegrambots.meta.api.objects.User telegramUser = new User(userId, firstName, false);
        telegramUser.setLanguageCode(lang);

        org.telegram.bot.domain.entities.User user = userMapper.toUser(telegramUser);

        assertEquals(userId, user.getUserId());
        assertEquals(firstName, user.getUsername());
        assertEquals(lang, user.getLang());
    }

    @Test
    void toUserTest() {
        final long userId = 1L;
        final String firstName = "firstName";
        final String username = "username";
        final String lang = "en";

        org.telegram.telegrambots.meta.api.objects.User telegramUser = new User(userId, firstName, false);
        telegramUser.setLanguageCode(lang);
        telegramUser.setUserName(username);

        org.telegram.bot.domain.entities.User user = userMapper.toUser(telegramUser);

        assertEquals(userId, user.getUserId());
        assertEquals(username, user.getUsername());
        assertEquals(lang, user.getLang());
    }

}