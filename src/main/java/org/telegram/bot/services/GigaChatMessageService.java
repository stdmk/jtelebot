package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.GigaChatMessage;
import org.telegram.bot.domain.entities.User;

import java.util.List;

public interface GigaChatMessageService {
    List<GigaChatMessage> getMessages(Chat chat);
    List<GigaChatMessage> getMessages(User user);
    void update(List<GigaChatMessage> messages);
    void reset(Chat chat);
    void reset(User user);
}
