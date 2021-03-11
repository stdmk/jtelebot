package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Alias;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.AliasRepository;
import org.telegram.bot.services.AliasService;

import java.util.List;

@Service
@AllArgsConstructor
public class AliasServiceImpl implements AliasService {

    private final Logger log = LoggerFactory.getLogger(AliasServiceImpl.class);

    private final AliasRepository aliasRepository;

    @Override
    public Alias get(Long aliasId) {
        log.debug("Request to get Alias by aliasId: {}", aliasId);
        return aliasRepository.findById(aliasId).orElse(null);
    }

    @Override
    public Alias get(Chat chat, User user, Long aliasId) {
        log.debug("Request to get Alias by aliasId {}, chat {} and user {}", aliasId, chat, user);
        return aliasRepository.findByChatAndUserAndId(chat, user, aliasId);
    }

    @Override
    public Alias get(Chat chat, User user, String name) {
        log.debug("Request to get Alias by name: {} for chat {} and user {}", name, chat.getChatId(), user.getUserId());
        return aliasRepository.findByChatAndUserAndNameIgnoreCase(chat, user, name);
    }

    @Override
    public List<Alias> get(Chat chat, User user) {
        log.debug("Request to get UserCity by User: {} and chatId: {}", user, chat);
        return aliasRepository.findByChatAndUser(chat, user);
    }

    @Override
    public Alias save(Alias alias) {
        log.debug("Request to save UserCity: {}", alias);
        return aliasRepository.save(alias);
    }

    @Override
    public Boolean remove(Chat chat, User user, Long aliasId) {
        log.debug("Request to delete Alias by id: {}", aliasId);

        Alias alias = get(chat, user, aliasId);

        if (alias != null) {
            aliasRepository.delete(alias);
            return true;
        }

        return false;
    }

    @Override
    public Boolean remove(Chat chat, User user, String name) {
        log.debug("Request to delete Alias by name: {}", name);

        Alias alias = get(chat, user, name);

        if (alias != null) {
            aliasRepository.delete(alias);
            return true;
        }

        return false;
    }
}
