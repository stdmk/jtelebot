package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CustomReactionDayStats;
import org.telegram.bot.domain.entities.User;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface CustomReactionDayStatsService {
    List<CustomReactionDayStats> get(Chat chat);
    List<CustomReactionDayStats> get(Chat chat, User user);
    List<CustomReactionDayStats> get(Chat chat, User user, Set<String> emojis);
    void save(Collection<CustomReactionDayStats> reactionDayStatsList);
    void removeAll();
}
