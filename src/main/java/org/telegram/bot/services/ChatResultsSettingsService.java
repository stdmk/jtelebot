package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatResultsSettings;

import java.util.List;

public interface ChatResultsSettingsService {
    ChatResultsSettings getAllEnabled(Chat chat);
    List<ChatResultsSettings> getAllEnabled();
    void save(ChatResultsSettings chatResultsSettings);
}
