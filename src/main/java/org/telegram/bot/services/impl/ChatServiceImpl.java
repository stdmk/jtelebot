package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.repositories.ChatRepository;
import org.telegram.bot.services.ChatService;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;

    @Override
    public Chat get(Long chatId) {
        log.debug("Request to get Chat by chatId: {} ", chatId);
        return chatRepository.findByChatId(chatId);
    }

    @Override
    public List<Chat> getAllGroups() {
        log.debug("Request to get all group-chats");
        return chatRepository.findByChatIdLessThan(0L);
    }

    @Override
    public Chat save(Chat chat) {
        log.debug("Request to save Chat: {} ", chat);
        return chatRepository.save(chat);
    }

    @Override
    public Integer getChatAccessLevel(Long chatId) {
        log.debug("Request to get chatLevel for chatId {} ", chatId);
        Chat chat = get(chatId);
        if (chat == null) {
            return AccessLevel.NEWCOMER.getValue();
        }

        return chat.getAccessLevel();
    }

    @Override
    public List<Chat> getChatsWithHolidays() {
        log.debug("Request to get Chats with holidays");
        return chatRepository.findDistinctChatWithHolidays();
    }
}
