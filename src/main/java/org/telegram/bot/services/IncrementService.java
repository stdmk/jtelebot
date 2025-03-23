package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Increment;
import org.telegram.bot.domain.entities.User;

import java.util.List;

public interface IncrementService {
    Increment get(Chat chat, User user, String name);
    List<Increment> get(Chat chat, User user);
    void save(Increment increment);
}
