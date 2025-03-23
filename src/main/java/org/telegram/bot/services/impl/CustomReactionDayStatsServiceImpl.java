package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CustomReactionDayStats;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.CustomReactionDayStatsRepository;
import org.telegram.bot.services.CustomReactionDayStatsService;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class CustomReactionDayStatsServiceImpl implements CustomReactionDayStatsService {

    private final CustomReactionDayStatsRepository customReactionDayStatsRepository;

    @Override
    public List<CustomReactionDayStats> get(Chat chat) {
        return customReactionDayStatsRepository.findByChat(chat);
    }

    @Override
    public List<CustomReactionDayStats> get(Chat chat, User user) {
        return customReactionDayStatsRepository.findByChatAndUser(chat, user);
    }

    @Override
    public List<CustomReactionDayStats> get(Chat chat, User user, Set<String> emojis) {
        return customReactionDayStatsRepository.findByChatAndUserAndEmojiIdIn(chat, user, emojis);
    }

    @Override
    public void save(Collection<CustomReactionDayStats> reactionDayStatsList) {
        customReactionDayStatsRepository.saveAll(reactionDayStatsList);
    }

    @Override
    @Transactional
    public void removeAll() {
        customReactionDayStatsRepository.deleteAll();
    }

}
