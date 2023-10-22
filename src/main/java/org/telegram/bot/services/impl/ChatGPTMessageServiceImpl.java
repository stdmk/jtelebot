package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatGPTMessage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.ChatGPTMessageRepository;
import org.telegram.bot.services.ChatGPTMessageService;
import org.telegram.bot.config.PropertiesConfig;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatGPTMessageServiceImpl implements ChatGPTMessageService {

    private final ChatGPTMessageRepository chatGPTMessageRepository;
    private final PropertiesConfig propertiesConfig;

    @Override
    public List<ChatGPTMessage> getMessages(Chat chat) {
        return chatGPTMessageRepository.findByChat(chat);
    }

    @Override
    public List<ChatGPTMessage> getMessages(User user) {
        return chatGPTMessageRepository.findByUserAndChat(user, new Chat().setChatId(user.getUserId()));
    }

    @Override
    public void update(List<ChatGPTMessage> messages) {
        Integer chatGPTContextSize = propertiesConfig.getChatGPTContextSize();
        if (messages.size() > chatGPTContextSize) {
            int deletingCount = messages.size() - chatGPTContextSize;
            List<ChatGPTMessage> chatGPTMessagesForDelete = messages
                    .stream()
                    .filter(chatGPTMessage -> chatGPTMessage.getId() != null)
                    .sorted(Comparator.comparingLong(ChatGPTMessage::getId))
                    .limit(deletingCount)
                    .collect(Collectors.toList());
            messages.removeAll(chatGPTMessagesForDelete);
            chatGPTMessageRepository.deleteAll(chatGPTMessagesForDelete);
        }

        chatGPTMessageRepository.saveAll(messages);
    }

    @Override
    @Transactional
    public void reset(Chat chat) {
        chatGPTMessageRepository.deleteAllByChat(chat);
    }

    @Override
    @Transactional
    public void reset(User user) {
        chatGPTMessageRepository.deleteAllByUserAndChat(user, new Chat().setChatId(user.getUserId()));
    }
}
