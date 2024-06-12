package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatGPTSettings;

public interface ChatGPTSettingRepository extends JpaRepository<ChatGPTSettings, Long> {
    ChatGPTSettings findByChat(Chat chat);
}
