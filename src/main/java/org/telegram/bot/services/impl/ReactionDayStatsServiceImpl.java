package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ReactionDayStats;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.ReactionDayStatsRepository;
import org.telegram.bot.services.ReactionDayStatsService;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class ReactionDayStatsServiceImpl implements ReactionDayStatsService {

    private final ReactionDayStatsRepository reactionDayStatsRepository;

    @Override
    public List<ReactionDayStats> get(Chat chat) {
        return reactionDayStatsRepository.findByChat(chat);
    }

    @Override
    public List<ReactionDayStats> get(Chat chat, User user) {
        return reactionDayStatsRepository.findByChatAndUser(chat, user);
    }

    @Override
    public List<ReactionDayStats> get(Chat chat, User user, Set<String> emojis) {
        return reactionDayStatsRepository.findByChatAndUserAndEmojiIn(chat, user, emojis);
    }

    @Override
    public void save(Collection<ReactionDayStats> reactionDayStatsList) {
        reactionDayStatsRepository.saveAll(reactionDayStatsList);
    }

    @Override
    @Transactional
    public void removeAll() {
        reactionDayStatsRepository.deleteAll();
    }

}
