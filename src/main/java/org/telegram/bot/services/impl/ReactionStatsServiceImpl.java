package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ReactionStats;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.ReactionStatsRepository;
import org.telegram.bot.services.ReactionStatsService;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class ReactionStatsServiceImpl implements ReactionStatsService {

    private final ReactionStatsRepository reactionStatsRepository;

    @Override
    public List<ReactionStats> get(Chat chat) {
        return reactionStatsRepository.findByChat(chat);
    }

    @Override
    public List<ReactionStats> get(Chat chat, User user) {
        return reactionStatsRepository.findByChatAndUser(chat, user);
    }

    @Override
    public List<ReactionStats> get(Chat chat, User user, Set<String> emojis) {
        return reactionStatsRepository.findByChatAndUserAndEmojiIn(chat, user, emojis);
    }

    @Override
    public void save(Collection<ReactionStats> reactionStats) {
        reactionStatsRepository.saveAll(reactionStats);
    }
}
