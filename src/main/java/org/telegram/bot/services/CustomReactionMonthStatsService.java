package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CustomReactionMonthStats;
import org.telegram.bot.domain.entities.User;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface CustomReactionMonthStatsService {
    List<CustomReactionMonthStats> get(Chat chat);
    List<CustomReactionMonthStats> get(Chat chat, User user);
    List<CustomReactionMonthStats> get(Chat chat, User user, Set<String> emojis);
    void save(Collection<CustomReactionMonthStats> reactionMonthStats);
    void removeAll();
}
