package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.repositories.CommandWaitingRepository;
import org.telegram.bot.services.CommandWaitingService;

@Service
@AllArgsConstructor
public class CommandWaitingServiceImpl implements CommandWaitingService {

    private final Logger log = LoggerFactory.getLogger(CommandWaitingServiceImpl.class);

    private final CommandWaitingRepository commandWaitingRepository;

    @Override
    public CommandWaiting get(Long chatId, Integer userId) {
        log.debug("Request to get CommandWaiting by chatId: {} and userId: {}", chatId, userId);
        return commandWaitingRepository.findByChatIdAndUserId(chatId, userId);
    }

    @Override
    public CommandWaiting save(CommandWaiting commandWaiting) {
        log.debug("Request to save CommandWaitingId {} ", commandWaiting);
        return commandWaitingRepository.save(commandWaiting);
    }

    @Override
    public void remove(CommandWaiting commandWaiting) {
        log.debug("Request to remove CommandWaiting {} ", commandWaiting);
        commandWaitingRepository.delete(commandWaiting);
    }
}
