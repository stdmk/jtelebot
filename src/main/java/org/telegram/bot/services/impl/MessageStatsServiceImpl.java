package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Message;
import org.telegram.bot.domain.entities.MessageStats;
import org.telegram.bot.repositories.MessageStatsRepository;
import org.telegram.bot.services.MessageService;
import org.telegram.bot.services.MessageStatsService;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class MessageStatsServiceImpl implements MessageStatsService {

    private final MessageService messageService;
    private final MessageStatsRepository messageStatsRepository;

    @Value("${today.minRepliesToGetTheTop:2}")
    private Integer minRepliesToGetTheTop;

    @Value("${today.messagesCountInTheTop:5}")
    private Integer messagesCountInTheTop;

    @Override
    public void incrementReplies(Message message) {
        getMessageStats(message).ifPresent(messageStats ->
                messageStatsRepository.save(messageStats.setReplies(messageStats.getReplies() + 1)));
    }

    @Override
    public void incrementReactions(Message message, int reactionsCount) {
        getMessageStats(message).ifPresent(messageStats ->
                messageStatsRepository.save(messageStats.setReactions(messageStats.getReactions() + reactionsCount)));
    }

    @Override
    public List<MessageStats> getByRepliesCountTop(Chat chat, LocalDate date) {
        return messageStatsRepository.findByMessageChatAndDateAndRepliesGreaterThanEqualOrderByReactionsDesc(
                chat,
                date,
                minRepliesToGetTheTop,
                PageRequest.of(0,  messagesCountInTheTop, Sort.by(Sort.Direction.DESC, "replies")));
    }

    @Override
    public List<MessageStats> getByReactionsCountTop(Chat chat, LocalDate date) {
        return messageStatsRepository.findByMessageChatAndDateAndReactionsGreaterThanEqualOrderByRepliesDesc(
                chat,
                date,
                minRepliesToGetTheTop,
                PageRequest.of(0,  messagesCountInTheTop, Sort.by(Sort.Direction.DESC, "reactions")));
    }

    @Override
    @Transactional
    public void removeAll(LocalDateTime expirationDateTime) {
        messageStatsRepository.deleteAllByMessageDateTimeGreaterThanEqual(expirationDateTime);
    }

    private Optional<MessageStats> getMessageStats(Message message) {
        MessageStats messageStats = messageStatsRepository.findByMessage(message);
        if (messageStats == null) {
            messageStats = messageStatsRepository.save(new MessageStats()
                    .setMessage(message)
                    .setReplies(0)
                    .setReactions(0)
                    .setDate(LocalDate.now()));
        }

        return Optional.of(messageStats);
    }

}
