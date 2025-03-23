package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.model.CustomReactionsStats;
import org.telegram.bot.services.CustomReactionDayStatsService;
import org.telegram.bot.services.CustomReactionMonthStatsService;
import org.telegram.bot.services.CustomReactionStatsService;
import org.telegram.bot.services.CustomReactionsStatsService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class CustomReactionsStatsServiceImpl implements CustomReactionsStatsService {

    private final CustomReactionStatsService customReactionStatsService;
    private final CustomReactionMonthStatsService customReactionMonthStatsService;
    private final CustomReactionDayStatsService customReactionDayStatsService;

    @Override
    public CustomReactionsStats get(Chat chat) {
        return new CustomReactionsStats()
                .setCustomReactionStats(customReactionStatsService.get(chat))
                .setCustomReactionMonthStats(customReactionMonthStatsService.get(chat))
                .setCustomReactionDayStats(customReactionDayStatsService.get(chat));
    }

    @Override
    public CustomReactionsStats get(Chat chat, User user) {
        return new CustomReactionsStats()
                .setCustomReactionStats(customReactionStatsService.get(chat, user))
                .setCustomReactionMonthStats(customReactionMonthStatsService.get(chat, user))
                .setCustomReactionDayStats(customReactionDayStatsService.get(chat, user));
    }

    @Override
    @Transactional
    public void update(Chat chat, User user, List<String> oldEmojis, List<String> newEmojis) {
        Map<String, CustomReactionStats> emojiReactionStatsMap = customReactionStatsService.get(
                        chat,
                        user,
                        Stream.concat(oldEmojis.stream(), newEmojis.stream()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(CustomReactionStats::getEmojiId, customReactionStats -> customReactionStats));
        Map<String, CustomReactionMonthStats> emojiReactionMonthStatsMap = customReactionMonthStatsService.get(
                        chat,
                        user,
                        Stream.concat(oldEmojis.stream(), newEmojis.stream()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(CustomReactionMonthStats::getEmojiId, customReactionStats -> customReactionStats));
        Map<String, CustomReactionDayStats> emojiReactionDayStatsMap = customReactionDayStatsService.get(
                        chat,
                        user,
                        Stream.concat(oldEmojis.stream(), newEmojis.stream()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(CustomReactionDayStats::getEmojiId, customReactionStats -> customReactionStats));

        for (String oldEmoji : oldEmojis) {
            CustomReactionStats customReactionStats = emojiReactionStatsMap.get(oldEmoji);
            if (customReactionStats == null) {
                customReactionStats = new CustomReactionStats()
                        .setChat(chat)
                        .setUser(user)
                        .setEmojiId(oldEmoji)
                        .setCount(1);
                emojiReactionStatsMap.put(customReactionStats.getEmojiId(), customReactionStats);
            }

            CustomReactionMonthStats customReactionMonthStats = emojiReactionMonthStatsMap.get(oldEmoji);
            if (customReactionMonthStats == null) {
                customReactionMonthStats = new CustomReactionMonthStats()
                        .setChat(chat)
                        .setUser(user)
                        .setEmojiId(oldEmoji)
                        .setCount(1);
                emojiReactionMonthStatsMap.put(customReactionMonthStats.getEmojiId(), customReactionMonthStats);
            }

            CustomReactionDayStats customReactionDayStats = emojiReactionDayStatsMap.get(oldEmoji);
            if (customReactionDayStats == null) {
                customReactionDayStats = new CustomReactionDayStats()
                        .setChat(chat)
                        .setUser(user)
                        .setEmojiId(oldEmoji)
                        .setCount(1);
                emojiReactionDayStatsMap.put(customReactionDayStats.getEmojiId(), customReactionDayStats);
            }

            customReactionStats.setCount(customReactionStats.getCount() - 1);
            customReactionMonthStats.setCount(customReactionMonthStats.getCount() - 1);
            customReactionDayStats.setCount(customReactionDayStats.getCount() - 1);
        }

        for (String newEmoji : newEmojis) {
            CustomReactionStats customReactionStats = emojiReactionStatsMap.get(newEmoji);
            if (customReactionStats == null) {
                customReactionStats = new CustomReactionStats()
                        .setChat(chat)
                        .setUser(user)
                        .setEmojiId(newEmoji)
                        .setCount(0);
                emojiReactionStatsMap.put(customReactionStats.getEmojiId(), customReactionStats);
            }

            CustomReactionMonthStats customReactionMonthStats = emojiReactionMonthStatsMap.get(newEmoji);
            if (customReactionMonthStats == null) {
                customReactionMonthStats = new CustomReactionMonthStats()
                        .setChat(chat)
                        .setUser(user)
                        .setEmojiId(newEmoji)
                        .setCount(0);
                emojiReactionMonthStatsMap.put(customReactionMonthStats.getEmojiId(), customReactionMonthStats);
            }

            CustomReactionDayStats customReactionDayStats = emojiReactionDayStatsMap.get(newEmoji);
            if (customReactionDayStats == null) {
                customReactionDayStats = new CustomReactionDayStats()
                        .setChat(chat)
                        .setUser(user)
                        .setEmojiId(newEmoji)
                        .setCount(0);
                emojiReactionDayStatsMap.put(customReactionDayStats.getEmojiId(), customReactionDayStats);
            }

            customReactionStats.setCount(customReactionStats.getCount() + 1);
            customReactionMonthStats.setCount(customReactionMonthStats.getCount() + 1);
            customReactionDayStats.setCount(customReactionDayStats.getCount() + 1);
        }

        customReactionStatsService.save(emojiReactionStatsMap.values());
        customReactionMonthStatsService.save(emojiReactionMonthStatsMap.values());
        customReactionDayStatsService.save(emojiReactionDayStatsMap.values());
    }

}
