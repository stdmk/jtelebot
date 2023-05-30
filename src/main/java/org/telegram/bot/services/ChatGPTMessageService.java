package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatGPTMessage;
import org.telegram.bot.domain.entities.User;

import java.util.List;

public interface ChatGPTMessageService {
    List<ChatGPTMessage> getMessages(Chat chat);
    List<ChatGPTMessage> getMessages(User user);
    void update(List<ChatGPTMessage> messages);
}
