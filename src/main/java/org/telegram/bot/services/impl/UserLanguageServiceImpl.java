package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserLanguage;
import org.telegram.bot.repositories.UserLanguageRepository;
import org.telegram.bot.services.UserLanguageService;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserLanguageServiceImpl implements UserLanguageService {

    private final UserLanguageRepository userLanguageRepository;

    @Override
    public void save(Chat chat, User user, String lang) {
        log.debug("Request to save UserLanguage by params: {} {} {}", chat, user, lang);

        UserLanguage userLanguage = this.get(chat, user);
        if (userLanguage == null) {
            userLanguage = new UserLanguage().setChat(chat).setUser(user);
        }
        userLanguage.setLang(lang);

        userLanguageRepository.save(userLanguage);
    }

    @Override
    public UserLanguage get(Chat chat, User user) {
        log.debug("Request to get UserLanguage by params: {} {}", chat, user);
        return userLanguageRepository.findByUserAndChat(user, chat);
    }
}
