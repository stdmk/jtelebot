package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserZodiac;
import org.telegram.bot.repositories.UserZodiacRepository;
import org.telegram.bot.services.UserZodiacService;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserZodiacServiceImpl implements UserZodiacService {

    private final UserZodiacRepository userZodiacRepository;

    @Override
    public UserZodiac get(Chat chat, User user) {
        log.debug("Request to get UserZodiac by Chat: {} and User: {}", chat, user);
        return userZodiacRepository.findByChatAndUser(chat, user);
    }

    @Override
    public void save(UserZodiac userZodiac) {
        log.debug("Request to save UserZodiac: {}", userZodiac);
        userZodiacRepository.save(userZodiac);
    }
}
