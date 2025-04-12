package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.model.ReactionsStats;
import org.telegram.bot.repositories.ReactionDayStatsRepository;
import org.telegram.bot.repositories.ReactionMonthStatsRepository;
import org.telegram.bot.repositories.ReactionStatsRepository;
import org.telegram.bot.services.ReactionsStatsService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class ReactionsStatsServiceImpl implements ReactionsStatsService {

    private final ReactionStatsRepository reactionStatsRepository;
    private final ReactionMonthStatsRepository reactionMonthStatsRepository;
    private final ReactionDayStatsRepository reactionDayStatsRepository;

    @Override
    public ReactionsStats get(Chat chat) {
        return new ReactionsStats()
                .setReactionStatsList(reactionStatsRepository.findByChat(chat))
                .setReactionMonthStatsList(reactionMonthStatsRepository.findByChat(chat))
                .setReactionDayStatsList(reactionDayStatsRepository.findByChat(chat));
    }

    @Override
    public ReactionsStats get(Chat chat, User user) {
        return new ReactionsStats()
                .setReactionStatsList(reactionStatsRepository.findByChatAndUser(chat, user))
                .setReactionMonthStatsList(reactionMonthStatsRepository.findByChatAndUser(chat, user))
                .setReactionDayStatsList(reactionDayStatsRepository.findByChatAndUser(chat, user));
    }

    @Override
    @Transactional
    public void update(Chat chat, User user, List<String> oldEmojis, List<String> newEmojis) {
        Map<String, ReactionStats> emojiReactionStatsMap = reactionStatsRepository.findByChatAndUserAndEmojiIn(
                        chat,
                        user,
                        Stream.concat(oldEmojis.stream(), newEmojis.stream()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ReactionStats::getEmoji, reactionStats -> reactionStats));
        Map<String, ReactionMonthStats> emojiReactionMonthStatsMap = reactionMonthStatsRepository.findByChatAndUserAndEmojiIn(
                        chat,
                        user,
                        Stream.concat(oldEmojis.stream(), newEmojis.stream()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ReactionMonthStats::getEmoji, reactionStats -> reactionStats));
        Map<String, ReactionDayStats> emojiReactionDayStatsMap = reactionDayStatsRepository.findByChatAndUserAndEmojiIn(
                        chat,
                        user,
                        Stream.concat(oldEmojis.stream(), newEmojis.stream()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ReactionDayStats::getEmoji, reactionStats -> reactionStats));

        for (String oldEmoji : oldEmojis) {
            ReactionStats reactionStats = emojiReactionStatsMap.get(oldEmoji);
            if (reactionStats == null) {
                reactionStats = new ReactionStats()
                        .setChat(chat)
                        .setUser(user)
                        .setEmoji(oldEmoji)
                        .setCount(1);
                emojiReactionStatsMap.put(reactionStats.getEmoji(), reactionStats);
            }

            ReactionMonthStats reactionMonthStats = emojiReactionMonthStatsMap.get(oldEmoji);
            if (reactionMonthStats == null) {
                reactionMonthStats = new ReactionMonthStats()
                        .setChat(chat)
                        .setUser(user)
                        .setEmoji(oldEmoji)
                        .setCount(1);
                emojiReactionMonthStatsMap.put(reactionMonthStats.getEmoji(), reactionMonthStats);
            }

            ReactionDayStats reactionDayStats = emojiReactionDayStatsMap.get(oldEmoji);
            if (reactionDayStats == null) {
                reactionDayStats = new ReactionDayStats()
                        .setChat(chat)
                        .setUser(user)
                        .setEmoji(oldEmoji)
                        .setCount(1);
                emojiReactionDayStatsMap.put(reactionDayStats.getEmoji(), reactionDayStats);
            }

            reactionStats.setCount(reactionStats.getCount() - 1);
            reactionMonthStats.setCount(reactionMonthStats.getCount() - 1);
            reactionDayStats.setCount(reactionDayStats.getCount() - 1);
        }

        for (String newEmoji : newEmojis) {
            ReactionStats reactionStats = emojiReactionStatsMap.get(newEmoji);
            if (reactionStats == null) {
                reactionStats = new ReactionStats()
                        .setChat(chat)
                        .setUser(user)
                        .setEmoji(newEmoji)
                        .setCount(0);
                emojiReactionStatsMap.put(reactionStats.getEmoji(), reactionStats);
            }

            ReactionMonthStats reactionMonthStats = emojiReactionMonthStatsMap.get(newEmoji);
            if (reactionMonthStats == null) {
                reactionMonthStats = new ReactionMonthStats()
                        .setChat(chat)
                        .setUser(user)
                        .setEmoji(newEmoji)
                        .setCount(0);
                emojiReactionMonthStatsMap.put(reactionMonthStats.getEmoji(), reactionMonthStats);
            }

            ReactionDayStats reactionDayStats = emojiReactionDayStatsMap.get(newEmoji);
            if (reactionDayStats == null) {
                reactionDayStats = new ReactionDayStats()
                        .setChat(chat)
                        .setUser(user)
                        .setEmoji(newEmoji)
                        .setCount(0);
                emojiReactionDayStatsMap.put(reactionDayStats.getEmoji(), reactionDayStats);
            }

            reactionStats.setCount(reactionStats.getCount() + 1);
            reactionMonthStats.setCount(reactionMonthStats.getCount() + 1);
            reactionDayStats.setCount(reactionDayStats.getCount() + 1);
        }

        reactionStatsRepository.saveAll(emojiReactionStatsMap.values());
        reactionMonthStatsRepository.saveAll(emojiReactionMonthStatsMap.values());
        reactionDayStatsRepository.saveAll(emojiReactionDayStatsMap.values());
    }

}
