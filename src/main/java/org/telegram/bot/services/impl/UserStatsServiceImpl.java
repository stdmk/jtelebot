package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.commands.Top;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.repositories.UserStatsRepository;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

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
    private final LastCommandService lastCommandService;

    @Override
    public UserStats get(Chat chat, User user) {
        log.debug("Request to get entity by Chat {} and {} User", chat, user);
        return userStatsRepository.findByChatAndUser(chat, user);
    }

    @Override
    public List<UserStats> getAllGroupStats() {
        log.debug("Request to get all group-stats entities");
        return userStatsRepository.findByChatChatIdLessThan(-1L);
    }

    @Override
    public UserStats save(UserStats userStats) {
        log.debug("Request to save UserStats {}", userStats);
        return userStatsRepository.save(userStats);
    }

    @Override
    public List<UserStats> save(List<UserStats> userStatsList) {
        log.debug("Request to save UserStats list {}", userStatsList);
        return userStatsRepository.saveAll(userStatsList);
    }

    @Override
    @Transactional
    public void updateEntitiesInfo(Message message, boolean editedMessage) {
        log.debug("Request to updates entities info");
        User user = updateUserInfo(message.getFrom());
        Chat chat = updateChatInfo(message.getChat());
        if (!editedMessage) {
            updateUserStats(chat, user, message);
        }
    }

    @Override
    public List<UserStats> getUserStatsListForChat(Chat chat) {
        log.debug("Request to get users of chat with id {}", chat);
        return userStatsRepository.findByChat(chat);
    }

    @Override
    public List<UserStats> getSortedUserStatsListForChat(Chat chat, String sortBy, int limit) {
        log.debug("Request to get users of chat with id {} and limit {} sort by {}", chat, limit, sortBy);
        return userStatsRepository.findByChat(chat, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, sortBy)));
    }

    @Override
    public List<SendMessage> clearMonthlyStats() {
        log.debug("Request to clear monthly stats of users");
        List<SendMessage> response = chatService.getAllGroups().stream()
                .map(chat -> new Top(this, userService, chatService, speechService).getTopByChat(chat))
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
        userStats.setNumberOfStickers(0);
        userStats.setNumberOfKarma(0);
        userStats.setNumberOfGoodness(0);
        userStats.setNumberOfWickedness(0);
    }

    @Override
    public void incrementUserStatsCommands(Chat chat, User user) {
        log.debug("Request to increment users stats commands using");

        UserStats userStats = get(chat, user);
        userStats.setNumberOfCommands(userStats.getNumberOfCommands() + 1);
        userStats.setNumberOfAllCommands(userStats.getNumberOfAllCommands() + 1);

        save(userStats);
    }

    @Override
    public void incrementUserStatsCommands(Chat chat, User user, CommandProperties commandProperties) {
        LastCommand lastCommand = lastCommandService.get(chat);
        if (lastCommand == null) {
            lastCommand = new LastCommand();
            lastCommand.setChat(chat);
        }
        lastCommand.setCommandProperties(commandProperties);
        lastCommandService.save(lastCommand);

        incrementUserStatsCommands(chat, user);
    }

    private User updateUserInfo(org.telegram.telegrambots.meta.api.objects.User userFrom) {
        String username = userFrom.getUserName();
        if (username == null) {
            username = userFrom.getFirstName();
        }

        Long userId = userFrom.getId();
        User user = userService.get(userId);

        if (user == null) {
            user = new User();
            user.setUserId(userId);
            user.setUsername(username);
            user.setAccessLevel(AccessLevel.NEWCOMER.getValue());
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
            chat.setAccessLevel(AccessLevel.NEWCOMER.getValue());
            chat = chatService.save(chat);
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
            userStats.setNumberOfKarma(0);
            userStats.setNumberOfAllKarma(0L);
            userStats.setNumberOfGoodness(0);
            userStats.setNumberOfAllGoodness(0L);
            userStats.setNumberOfWickedness(0);
            userStats.setNumberOfAllWickedness(0L);
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
