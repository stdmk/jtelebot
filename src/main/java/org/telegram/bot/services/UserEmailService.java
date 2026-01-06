package org.telegram.bot.services;

import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserEmail;

import java.util.List;

public interface UserEmailService {
    UserEmail save(UserEmail userEmail);
    UserEmail get(List<String> email);
    UserEmail get(User user);
    List<UserEmail> getByUsers(List<User> users);
    void remove(UserEmail userEmail);
}
