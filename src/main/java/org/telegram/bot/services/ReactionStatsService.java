package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ReactionStats;
import org.telegram.bot.domain.entities.User;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ReactionStatsService {
    List<ReactionStats> get(Chat chat);
    List<ReactionStats> get(Chat chat, User user);
    List<ReactionStats> get(Chat chat, User user, Set<String> emojis);
    void save(Collection<ReactionStats> reactionStats);
}
