package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatResultsSettings;
import org.telegram.bot.repositories.ChatResultsSettingsRepository;
import org.telegram.bot.services.ChatResultsSettingsService;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class ChatResultsSettingsServiceImpl implements ChatResultsSettingsService {

    private final ChatResultsSettingsRepository chatResultsSettingsRepository;

    @Override
    public ChatResultsSettings getAllEnabled(Chat chat) {
        log.debug("Request to get ChatResultsSettings by Chat {}", chat);
        return chatResultsSettingsRepository.findByChat(chat);
    }

    @Override
    public List<ChatResultsSettings> getAllEnabled() {
        log.debug("Request to get all enabled ChatResultsSettings");
        return chatResultsSettingsRepository.findByEnabled(true);
    }

    @Override
    public void save(ChatResultsSettings chatResultsSettings) {
        log.debug("Request to save ChatResultsSettings: {}", chatResultsSettings);
        chatResultsSettingsRepository.save(chatResultsSettings);
    }

}
