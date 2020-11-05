package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.commands.Top;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.LastMessage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.enums.AccessLevels;
import org.telegram.bot.repositories.UserStatsRepository;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserStatsServiceImpl implements UserStatsService {

    private final Logger log = LoggerFactory.getLogger(UserStatsServiceImpl.class);

    private final UserStatsRepository userStatsRepository;

    private final UserService userService;
    private final ChatService chatService;
    private final LastMessageService lastMessageService;
    private final SpeechService speechService;

    @Override
    public UserStats get(Long chatId, Integer userId) {
        log.debug("Request to get entity by Chat {} and {} User", chatId, userId);
        return userStatsRepository.findByChatIdAndUserUserId(chatId, userId);
    }

    @Override
    public List<UserStats> getAllGroupStats() {
        log.debug("Request to get all group-stats entities");
        return userStatsRepository.findByChatIdLessThan(-1L);
    }

    @Override
    public UserStats save(UserStats userStats) {
        log.debug("Request to save UserStats {}", userStats);
        return userStatsRepository.save(userStats);
    }

    @Override
    @Transactional
    public void updateEntitiesInfo(Message message) {
        log.debug("Request to updates entities info");
        User user = updateUserInfo(message.getFrom());
        Chat chat = updateChatInfo(message.getChat());
        updateUserStats(chat, user, message);
    }

    @Override
    public List<UserStats> getUsersByChatId(Long chatId) {
        log.debug("Request to get users of chat with id {}", chatId);
        return userStatsRepository.findByChatId(chatId);
    }

    @Override
    public List<SendMessage> clearMonthlyStats() {
        log.debug("Request to clear monthly stats of users");
        List<SendMessage> response = chatService.getAllGroups().stream()
                .map(chat -> new Top(this, userService, speechService).getTopByChatId(chat.getChatId()))
                .collect(Collectors.toList());

        userStatsRepository.saveAll(getAllGroupStats().stream()
                .peek(this::clearUserStatsFields)
                .collect(Collectors.toList()));

        return response;
    }

    private void clearUserStatsFields(UserStats userStats) {
        userStats.setNumberOfMessages(0);
        userStats.setNumberOfPhotos(0);
        userStats.setNumberOfAnimations(0);
        userStats.setNumberOfAudio(0);
        userStats.setNumberOfDocuments(0);
        userStats.setNumberOfVideos(0);
        userStats.setNumberOfVideoNotes(0);
        userStats.setNumberOfVoices(0);
        userStats.setNumberOfCommands(0);
    }

    @Override
    public void incrementUserStatsCommands(Long chatId, Integer userId) {
        log.debug("Request to increment users stats commands using");

        UserStats userStats = get(chatId, userId);
        userStats.setNumberOfCommands(userStats.getNumberOfCommands() + 1);
        userStats.setNumberOfAllCommands(userStats.getNumberOfAllCommands() + 1);

        save(userStats);
    }

    private User updateUserInfo(org.telegram.telegrambots.meta.api.objects.User userFrom) {
        String username = userFrom.getUserName();
        if (username == null) {
            username = userFrom.getFirstName();
        }

        Integer userId = userFrom.getId();
        User user = userService.get(userId);

        if (user == null) {
            user = new User();
            user.setUserId(userId);
            user.setUsername(username);
            user.setAccessLevel(AccessLevels.NEWCOMER.getValue());
            user = userService.save(user);
        }

        else if (!user.getUsername().equals(username)) {
            user.setUsername(username);
            user = userService.save(user);
        }

        return user;
    }

    private Chat updateChatInfo(org.telegram.telegrambots.meta.api.objects.Chat chatFrom) {
        Long chatId = chatFrom.getId();

        String chatName;
        if (chatId > 0) {
            chatName = chatFrom.getUserName();
            if (chatName == null) {
                chatName = chatFrom.getFirstName();
            }
        } else {
            chatName = chatFrom.getTitle();
            if (chatName == null) {
                chatName = "";
            }
        }

        Chat chat = chatService.get(chatId);
        if (chat == null) {
            chat = new Chat();
            chat.setChatId(chatId);
            chat.setName(chatName);
            chat.setAccessLevel(AccessLevels.NEWCOMER.getValue());
            chat = chatService.save(chat);
        }

        else if (!chat.getName().equals(chatName)) {
            chat.setName(chatName);
            chat = chatService.save(chat);
        }

        return chat;
    }

    private void updateUserStats(Chat chat, User user, Message message) {
        UserStats userStats = get(chat.getChatId(), user.getUserId());
        if (userStats == null) {
            LastMessage lastMessage = new LastMessage();

            userStats = new UserStats();
            userStats.setChatId(chat.getChatId());
            userStats.setUser(user);
            userStats.setNumberOfMessages(0);
            userStats.setNumberOfAllMessages(0L);
            userStats.setNumberOfStickers(0);
            userStats.setNumberOfAllStickers(0L);
            userStats.setNumberOfPhotos(0);
            userStats.setNumberOfAllPhotos(0L);
            userStats.setNumberOfAnimations(0);
            userStats.setNumberOfAllAnimations(0L);
            userStats.setNumberOfAudio(0);
            userStats.setNumberOfAllAudio(0L);
            userStats.setNumberOfDocuments(0);
            userStats.setNumberOfAllDocuments(0L);
            userStats.setNumberOfVideos(0);
            userStats.setNumberOfAllVideos(0L);
            userStats.setNumberOfVideoNotes(0);
            userStats.setNumberOfAllVideoNotes(0L);
            userStats.setNumberOfVoices(0);
            userStats.setNumberOfAllVoices(0L);
            userStats.setNumberOfCommands(0);
            userStats.setNumberOfAllCommands(0L);
            userStats.setLastMessage(lastMessageService.update(lastMessage, message));
        }

        if (message.hasText()) {
            userStats.setNumberOfMessages(userStats.getNumberOfMessages() + 1);
            userStats.setNumberOfAllMessages(userStats.getNumberOfAllMessages() + 1);
            LastMessage lastMessage = userStats.getLastMessage();
            userStats.setLastMessage(lastMessageService.update(lastMessage, message));
        }
        else if (message.hasSticker()) {
            userStats.setNumberOfStickers(userStats.getNumberOfStickers() + 1);
            userStats.setNumberOfAllStickers(userStats.getNumberOfAllStickers() + 1);
        }
        else if (message.hasPhoto()) {
            userStats.setNumberOfPhotos(userStats.getNumberOfPhotos() + 1);
            userStats.setNumberOfAllPhotos(userStats.getNumberOfAllPhotos() + 1);
        }
        else if (message.hasAnimation()) {
            userStats.setNumberOfAnimations(userStats.getNumberOfAnimations() + 1);
            userStats.setNumberOfAllAnimations(userStats.getNumberOfAllAnimations() + 1);
        }
        else if (message.hasAudio()) {
            userStats.setNumberOfAudio(userStats.getNumberOfAudio() + 1);
            userStats.setNumberOfAllAudio(userStats.getNumberOfAllAudio() + 1);
        }
        else if (message.hasDocument()) {
            userStats.setNumberOfDocuments(userStats.getNumberOfDocuments() + 1);
            userStats.setNumberOfAllDocuments(userStats.getNumberOfAllDocuments() + 1);
        }
        else if (message.hasVideo()) {
            userStats.setNumberOfVideos(userStats.getNumberOfVideos() + 1);
            userStats.setNumberOfAllVideos(userStats.getNumberOfAllVideos() + 1);
        }
        else if (message.hasVideoNote()) {
            userStats.setNumberOfVideoNotes(userStats.getNumberOfVideoNotes() + 1);
            userStats.setNumberOfAllVideoNotes(userStats.getNumberOfAllVideoNotes() + 1);
        }
        else if (message.hasVoice()) {
            userStats.setNumberOfVoices(userStats.getNumberOfVoices() + 1);
            userStats.setNumberOfAllVoices(userStats.getNumberOfAllVoices() + 1);
        }

        save(userStats);
    }
}
