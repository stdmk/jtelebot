package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.LastMessage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.enums.AccessLevels;
import org.telegram.bot.repositories.UserStatsRepository;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.LastMessageService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
@AllArgsConstructor
public class UserStatsServiceImpl implements UserStatsService {

    private final Logger log = LoggerFactory.getLogger(UserStatsServiceImpl.class);

    private final UserStatsRepository userStatsRepository;

    private final UserService userService;
    private final ChatService chatService;
    private final LastMessageService lastMessageService;

    @Override
    public UserStats get(Chat chat, User user) {
        log.debug("Request to get entity by Chat {} and {} User", chat, user);

        return userStatsRepository.findByChatAndUser(chat, user);
    }

    @Override
    public UserStats save(UserStats userStats) {
        log.debug("Request to save UserStats {}", userStats);

        return userStatsRepository.save(userStats);
    }

    @Override
    public void updateEntitiesInfo(Update update) {
        log.debug("Request to updates entities info");
        User user = updateUserInfo(update.getMessage().getFrom());
        Chat chat = updateChatInfo(update.getMessage().getChat());
        updateUserStats(chat, user, update.getMessage());
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
        }

        else if (!chat.getName().equals(chatName)) {
            chat.setName(chatName);
            chat = chatService.save(chat);
        }

        return chat;
    }

    private void updateUserStats(Chat chat, User user, Message message) {
        UserStats userStats = get(chat, user);
        if (userStats == null) {
            LastMessage lastMessage = new LastMessage();

            userStats = new UserStats();
            userStats.setChat(chat);
            userStats.setUser(user);
            userStats.setNumberOfMessages(1);
            userStats.setNumberOfAllMessages(1L);
            userStats.setLastMessage(lastMessageService.update(lastMessage, message));
        }
        LastMessage lastMessage = userStats.getLastMessage();

        userStats.setNumberOfMessages(userStats.getNumberOfMessages() + 1);
        userStats.setNumberOfAllMessages(userStats.getNumberOfAllMessages() + 1);
        userStats.setLastMessage(lastMessageService.update(lastMessage, message));

        save(userStats);
    }
}
