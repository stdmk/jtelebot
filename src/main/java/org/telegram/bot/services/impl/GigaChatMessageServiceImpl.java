package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.GigaChatMessage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.GigaChatMessageRepository;
import org.telegram.bot.services.GigaChatMessageService;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GigaChatMessageServiceImpl implements GigaChatMessageService {

    private final GigaChatMessageRepository gigaChatMessageRepository;
    private final PropertiesConfig propertiesConfig;

    @Override
    public List<GigaChatMessage> getMessages(Chat chat) {
        return gigaChatMessageRepository.findByChat(chat);
    }

    @Override
    public List<GigaChatMessage> getMessages(User user) {
        return gigaChatMessageRepository.findByUserAndChat(user, new Chat().setChatId(user.getUserId()));
    }

    @Override
    public void update(List<GigaChatMessage> messages) {
        Integer chatGPTContextSize = propertiesConfig.getChatGPTContextSize();
        if (messages.size() > chatGPTContextSize) {
            int deletingCount = messages.size() - chatGPTContextSize;
            List<GigaChatMessage> chatGPTMessagesForDelete = messages
                    .stream()
                    .filter(chatGPTMessage -> chatGPTMessage.getId() != null)
                    .sorted(Comparator.comparingLong(GigaChatMessage::getId))
                    .limit(deletingCount)
                    .toList();
            messages.removeAll(chatGPTMessagesForDelete);
            gigaChatMessageRepository.deleteAll(chatGPTMessagesForDelete);
        }

        gigaChatMessageRepository.saveAll(messages);
    }

    @Override
    @Transactional
    public void reset(Chat chat) {
        gigaChatMessageRepository.deleteAllByChat(chat);
    }

    @Override
    @Transactional
    public void reset(User user) {
        gigaChatMessageRepository.deleteAllByUserAndChat(user, new Chat().setChatId(user.getUserId()));
    }
}
