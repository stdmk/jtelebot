package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.TvChannel;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserTv;
import org.telegram.bot.repositories.UserTvRepository;
import org.telegram.bot.services.UserTvService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserTvServiceImpl implements UserTvService {

    private final UserTvRepository userTvRepository;

    @Override
    public UserTv get(Long userTvId) {
        log.debug("Request to get UserTv by userTvId: {}", userTvId);
        return userTvRepository.findById(userTvId).orElse(null);
    }

    @Override
    public UserTv get(Chat chat, User user, TvChannel tvChannel) {
        log.debug("Request to get UserTvs by User: {} and Chat: {} and TvChannel {}", user, chat, tvChannel);
        return userTvRepository.findByChatAndUserAndTvChannel(chat, user, tvChannel);
    }

    @Override
    public List<UserTv> get(Chat chat, User user) {
        log.debug("Request to get UserTvs by User: {} and Chat: {}", user, chat);
        return userTvRepository.findByChatAndUser(chat, user);
    }

    @Override
    public UserTv save(UserTv userTv) {
        log.debug("Request to save UserTv: {}", userTv);
        return userTvRepository.save(userTv);
    }

    @Override
    public void remove(UserTv userTv) {
        log.debug("Request to delete UserTv: {}", userTv);
        userTvRepository.delete(userTv);
    }
}
