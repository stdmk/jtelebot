package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatLanguage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.ChatLanguageRepository;
import org.telegram.bot.services.ChatLanguageService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatLanguageServiceImpl implements ChatLanguageService {

    private final ChatLanguageRepository chatLanguageRepository;

    @Override
    public void save(Chat chat, String lang) {
        ChatLanguage chatLanguage = chatLanguageRepository.findByChat(chat);
        if (chatLanguage == null) {
            chatLanguage = new ChatLanguage().setChat(chat);
        }
        chatLanguage.setLang(lang);

        log.debug("Request to save ChatLanguage: {}", chatLanguage);

        chatLanguageRepository.save(chatLanguage);
    }

    @Override
    public ChatLanguage get(Chat chat) {
        log.debug("Request to get ChatLanguage by chatId: {}", chat);
        return chatLanguageRepository.findByChat(chat);
    }
}
