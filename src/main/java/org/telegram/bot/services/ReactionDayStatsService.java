package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ReactionDayStats;
import org.telegram.bot.domain.entities.User;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ReactionDayStatsService {
    List<ReactionDayStats> get(Chat chat);
    List<ReactionDayStats> get(Chat chat, User user);
    List<ReactionDayStats> get(Chat chat, User user, Set<String> emojis);
    void save(Collection<ReactionDayStats> reactionDayStatsList);
    void removeAll();
}
