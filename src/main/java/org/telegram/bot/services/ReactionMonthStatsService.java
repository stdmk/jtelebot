package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ReactionMonthStats;
import org.telegram.bot.domain.entities.User;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ReactionMonthStatsService {
    List<ReactionMonthStats> get(Chat chat);
    List<ReactionMonthStats> get(Chat chat, User user);
    List<ReactionMonthStats> get(Chat chat, User user, Set<String> emojis);
    void save(Collection<ReactionMonthStats> reactionMonthStats);
    void removeAll();
}
