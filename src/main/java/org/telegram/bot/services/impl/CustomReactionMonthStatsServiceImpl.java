package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CustomReactionMonthStats;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.CustomReactionMonthStatsRepository;
import org.telegram.bot.services.CustomReactionMonthStatsService;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class CustomReactionMonthStatsServiceImpl implements CustomReactionMonthStatsService {

    private final CustomReactionMonthStatsRepository customReactionMonthStatsRepository;

    @Override
    public List<CustomReactionMonthStats> get(Chat chat) {
        return customReactionMonthStatsRepository.findByChat(chat);
    }

    @Override
    public List<CustomReactionMonthStats> get(Chat chat, User user) {
        return customReactionMonthStatsRepository.findByChatAndUser(chat, user);
    }

    @Override
    public List<CustomReactionMonthStats> get(Chat chat, User user, Set<String> emojis) {
        return customReactionMonthStatsRepository.findByChatAndUserAndEmojiIdIn(chat, user, emojis);
    }

    @Override
    public void save(Collection<CustomReactionMonthStats> reactionMonthStats) {
        customReactionMonthStatsRepository.saveAll(reactionMonthStats);
    }

    @Override
    @Transactional
    public void removeAll() {
        customReactionMonthStatsRepository.deleteAll();
    }

}
