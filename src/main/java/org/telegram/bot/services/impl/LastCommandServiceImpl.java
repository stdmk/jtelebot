package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.LastCommand;
import org.telegram.bot.repositories.LastCommandRepository;
import org.telegram.bot.services.LastCommandService;

@Service
@AllArgsConstructor
public class LastCommandServiceImpl implements LastCommandService {

    private final Logger log = LoggerFactory.getLogger(LastCommandServiceImpl.class);

    private final LastCommandRepository lastCommandRepository;

    @Override
    public LastCommand get(Long id) {
        log.debug("Request to get LastCommand by id: {} ", id);
        return lastCommandRepository.findById(id).orElse(null);
    }

    @Override
    public LastCommand get(Chat chat) {
        log.debug("Request to get LastCommand for chat: {} ", chat);
        return lastCommandRepository.findByChat(chat);
    }

    @Override
    public LastCommand save(LastCommand lastCommand) {
        log.debug("Request to save LastCommand: {} ", lastCommand);
        return lastCommandRepository.save(lastCommand);
    }
}
