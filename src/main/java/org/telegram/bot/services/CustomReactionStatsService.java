package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CustomReactionStats;
import org.telegram.bot.domain.entities.User;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface CustomReactionStatsService {
    List<CustomReactionStats> get(Chat chat);
    List<CustomReactionStats> get(Chat chat, User user);
    List<CustomReactionStats> get(Chat chat, User user, Set<String> emojis);
    void save(Collection<CustomReactionStats> customReactionStats);
}
