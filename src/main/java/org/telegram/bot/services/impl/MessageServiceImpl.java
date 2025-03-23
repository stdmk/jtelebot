package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Message;
import org.telegram.bot.repositories.MessageRepository;
import org.telegram.bot.services.MessageService;

import javax.transaction.Transactional;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;

    @Override
    public Message get(int messageId) {
        return messageRepository.findById(messageId).orElse(null);
    }

    @Override
    public void save(org.telegram.bot.domain.model.request.Message message) {
        if (message.hasReactions()) {
            return;
        }

        messageRepository.save(toMessageEntity(message));
    }

    @Override
    @Transactional
    public void removeAll(LocalDateTime expirationDateTime) {
        messageRepository.deleteAllByDateTimeGreaterThanEqual(expirationDateTime);
    }

    private Message toMessageEntity(org.telegram.bot.domain.model.request.Message message) {
        return new org.telegram.bot.domain.entities.Message()
                .setChat(message.getChat())
                .setUser(message.getUser())
                .setMessageContentType(message.getMessageContentType())
                .setMessageId(message.getMessageId())
                .setText(message.getText())
                .setDateTime(message.getDateTime());
    }

}
