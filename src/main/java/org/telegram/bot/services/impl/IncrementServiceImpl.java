package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Increment;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.IncrementRepository;
import org.telegram.bot.services.IncrementService;

import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Service
public class IncrementServiceImpl implements IncrementService {

    private final IncrementRepository incrementRepository;

    @Override
    public Increment get(Chat chat, User user, String name) {
        return incrementRepository.findByChatAndUserAndName(chat, user, name.toLowerCase(Locale.ROOT));
    }

    @Override
    public List<Increment> get(Chat chat, User user) {
        return incrementRepository.findByChatAndUser(chat, user);
    }

    @Override
    public void save(Increment increment) {
        incrementRepository.save(increment);
    }

    @Override
    public void remove(Increment increment) {
        incrementRepository.delete(increment);
    }
}
