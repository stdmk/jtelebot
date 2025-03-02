package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatResultsSettings;

import java.util.List;

/**
 * Spring Data repository for the ChatResultsSettings entity.
 */
public interface ChatResultsSettingsRepository extends JpaRepository<ChatResultsSettings, Long> {
    List<ChatResultsSettings> findByEnabled(boolean enabled);
    ChatResultsSettings findByChat(Chat chat);
}
