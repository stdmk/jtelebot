package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ReactionMonthStats;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.ReactionMonthStatsRepository;
import org.telegram.bot.services.ReactionMonthStatsService;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class ReactionMonthStatsServiceImpl implements ReactionMonthStatsService {

    private final ReactionMonthStatsRepository reactionMonthStatsRepository;

    @Override
    public List<ReactionMonthStats> get(Chat chat) {
        return reactionMonthStatsRepository.findByChat(chat);
    }

    @Override
    public List<ReactionMonthStats> get(Chat chat, User user) {
        return reactionMonthStatsRepository.findByChatAndUser(chat, user);
    }

    @Override
    public List<ReactionMonthStats> get(Chat chat, User user, Set<String> emojis) {
        return reactionMonthStatsRepository.findByChatAndUserAndEmojiIn(chat, user, emojis);
    }

    @Override
    public void save(Collection<ReactionMonthStats> reactionMonthStats) {
        reactionMonthStatsRepository.saveAll(reactionMonthStats);
    }

    @Override
    @Transactional
    public void removeAll() {
        reactionMonthStatsRepository.deleteAll();
    }

}
