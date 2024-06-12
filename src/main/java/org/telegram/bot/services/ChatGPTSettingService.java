package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatGPTSettings;

public interface ChatGPTSettingService {
    ChatGPTSettings get(Chat chat);
    void save(ChatGPTSettings chatGPTSettings);
}
