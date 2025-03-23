package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CustomReactionStats;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.CustomReactionStatsRepository;
import org.telegram.bot.services.CustomReactionStatsService;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class CustomReactionStatsServiceImpl implements CustomReactionStatsService {

    private final CustomReactionStatsRepository customReactionStatsRepository;

    @Override
    public List<CustomReactionStats> get(Chat chat) {
        return customReactionStatsRepository.findByChat(chat);
    }

    @Override
    public List<CustomReactionStats> get(Chat chat, User user) {
        return customReactionStatsRepository.findByChatAndUser(chat, user);
    }

    @Override
    public List<CustomReactionStats> get(Chat chat, User user, Set<String> emojis) {
        return customReactionStatsRepository.findByChatAndUserAndEmojiIdIn(chat, user, emojis);
    }

    @Override
    public void save(Collection<CustomReactionStats> customReactionStats) {
        customReactionStatsRepository.saveAll(customReactionStats);
    }
}
