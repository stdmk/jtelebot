package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.LastCommand;
import org.telegram.bot.repositories.LastCommandRepository;
import org.telegram.bot.services.LastCommandService;

@Service
@RequiredArgsConstructor
@Slf4j
public class LastCommandServiceImpl implements LastCommandService {

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
    public void save(LastCommand lastCommand) {
        log.debug("Request to save LastCommand: {} ", lastCommand);
        lastCommandRepository.save(lastCommand);
    }
}
