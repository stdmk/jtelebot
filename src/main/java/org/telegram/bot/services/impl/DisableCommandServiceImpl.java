package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.DisableCommand;
import org.telegram.bot.repositories.DisableCommandRepository;
import org.telegram.bot.services.DisableCommandService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisableCommandServiceImpl implements DisableCommandService {

    private final DisableCommandRepository disableCommandRepository;

    @Override
    public List<DisableCommand> getByChat(Chat chat) {
        log.debug("Request to get list of DisableCommandList by Chat: {}", chat);
        return disableCommandRepository.findByChat(chat);
    }

    @Override
    public DisableCommand get(Long id) {
        log.debug("Request to get DisableCommandList by id: {}", id);
        return disableCommandRepository.findById(id).orElse(null);
    }

    @Override
    public DisableCommand get(Chat chat, CommandProperties commandProperties) {
        log.debug("Request to get DisableCommandList by chat: {} and CommandProperties {}", chat, commandProperties);
        return disableCommandRepository.findByChatAndCommandProperties(chat, commandProperties);
    }

    @Override
    public void save(DisableCommand disableCommand) {
        log.debug("Request to save DisableCommandList: {}", disableCommand);
        disableCommandRepository.save(disableCommand);
    }

    @Override
    public void remove(DisableCommand disableCommand) {
        log.debug("Request to delete DisableCommandList: {}", disableCommand);
        disableCommandRepository.delete(disableCommand);
    }
}
