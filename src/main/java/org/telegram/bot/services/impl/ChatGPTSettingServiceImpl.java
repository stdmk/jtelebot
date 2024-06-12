package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatGPTSettings;
import org.telegram.bot.repositories.ChatGPTSettingRepository;
import org.telegram.bot.services.ChatGPTSettingService;

@RequiredArgsConstructor
@Service
@Slf4j
public class ChatGPTSettingServiceImpl implements ChatGPTSettingService {

    private final ChatGPTSettingRepository chatGPTSettingRepository;

    @Override
    public ChatGPTSettings get(Chat chat) {
        return chatGPTSettingRepository.findByChat(chat);
    }

    @Override
    public void save(ChatGPTSettings chatGPTSettings) {
        chatGPTSettingRepository.save(chatGPTSettings);
    }
}


