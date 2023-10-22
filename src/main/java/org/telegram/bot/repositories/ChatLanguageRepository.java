package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatLanguage;

/**
 * Spring Data repository for the ChatLanguage entity.
 */
public interface ChatLanguageRepository extends JpaRepository<ChatLanguage, Long> {
    ChatLanguage findByChat(Chat chat);
}
