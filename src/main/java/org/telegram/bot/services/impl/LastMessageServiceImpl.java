package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.LastMessage;
import org.telegram.bot.repositories.LastMessageRepository;
import org.telegram.bot.services.LastMessageService;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.Instant;
import java.time.ZoneId;

@AllArgsConstructor
@Service
public class LastMessageServiceImpl implements LastMessageService {

    private final Logger log = LoggerFactory.getLogger(LastMessageServiceImpl.class);

    private final LastMessageRepository lastMessageRepository;

    @Override
    public LastMessage get(Long id) {
        log.debug("Request to get LastMessage by id: {} ", id);
        return lastMessageRepository.findById(id).orElse(new LastMessage());
    }

    @Override
    public LastMessage save(LastMessage lastMessage) {
        log.debug("Request to save LastMessage: {} ", lastMessage);
        return lastMessageRepository.save(lastMessage);
    }

    @Override
    public LastMessage update(LastMessage lastMessage, Message newMessage) {
        log.debug("Request to update LastMessage: {} by Message {}", lastMessage , newMessage);
        lastMessage.setMessageId(newMessage.getMessageId());
        lastMessage.setText(newMessage.getText());
        lastMessage.setDate(Instant.ofEpochSecond(newMessage.getDate()).atZone(ZoneId.systemDefault()).toLocalDateTime());

        return lastMessageRepository.save(lastMessage);
    }
}
