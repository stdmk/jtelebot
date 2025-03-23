package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.CustomReactionsStats;

import java.util.List;

public interface CustomReactionsStatsService {
    CustomReactionsStats get(Chat chat);
    CustomReactionsStats get(Chat chat, User user);
    void update(Chat chat, User user, List<String> oldEmojis, List<String> newEmojis);
}
