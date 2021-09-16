package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class UserStatsServiceImpl implements UserStatsService {

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
    public List<UserStats> getSortedUserStatsListWithKarmaForChat(Chat chat, String sortBy, int limit, boolean allKarma) {
        if (allKarma) {
            log.debug("Request to get users with allKarma of chat with id {} and limit {} sort by {}", chat, limit, sortBy);
            return userStatsRepository.findByChatAndNumberOfAllKarmaNot(chat, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, sortBy)), 0L);
        }

        log.debug("Request to get users with karma of chat with id {} and limit {} sort by {}", chat, limit, sortBy);
        return userStatsRepository.findByChatAndNumberOfKarmaNot(chat, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, sortBy)), 0);
    }

    @Override
    public List<SendMessage> clearMonthlyStats() {
        log.debug("Request to clear monthly stats of users");
        List<SendMessage> response = chatService.getAllGroups()
                .stream()
                .map(chat -> new Top(this, userService, chatService, speechService).getTopByChat(chat))
                .collect(Collectors.toList());

        userStatsRepository.saveAll(getAllGroupStats()
                .stream()
                .peek(this::clearUserStatsFields)
                .collect(Collectors.toList()));

        return response;
    }

    private void clearUserStatsFields(UserStats userStats) {
        userStats.setNumberOfMessages(0)
                .setNumberOfPhotos(0)
                .setNumberOfAnimations(0)
                .setNumberOfAudio(0)
                .setNumberOfDocuments(0)
                .setNumberOfVideos(0)
                .setNumberOfVideoNotes(0)
                .setNumberOfVoices(0)
                .setNumberOfCommands(0)
                .setNumberOfStickers(0)
                .setNumberOfKarma(0)
                .setNumberOfGoodness(0)
                .setNumberOfWickedness(0);
    }

    @Override
    public void incrementUserStatsCommands(Chat chat, User user) {
        log.debug("Request to increment users stats commands using");

        UserStats userStats = get(chat, user);
        userStats
                .setNumberOfCommands(userStats.getNumberOfCommands() + 1)
                .setNumberOfAllCommands(userStats.getNumberOfAllCommands() + 1);

        save(userStats);
    }

    @Override
    public void incrementUserStatsCommands(Chat chat, User user, CommandProperties commandProperties) {
        LastCommand lastCommand = lastCommandService.get(chat);
        if (lastCommand == null) {
            lastCommand = new LastCommand().setChat(chat);
        }

        lastCommand.setCommandProperties(commandProperties);
        lastCommandService.save(lastCommand);

        incrementUserStatsCommands(chat, user);
    }

    /**
     * Updating user info (username, access level)
     *
     * @param userFrom telegram User.
     * @return User entity.
     */
    private User updateUserInfo(org.telegram.telegrambots.meta.api.objects.User userFrom) {
        String username = userFrom.getUserName();
        if (username == null) {
            username = userFrom.getFirstName();
        }

        Long userId = userFrom.getId();
        User user = userService.get(userId);

        if (user == null) {
            user = new User()
                    .setUserId(userId)
                    .setUsername(username)
                    .setAccessLevel(AccessLevel.NEWCOMER.getValue());
            user = userService.save(user);
        }

        else if (!user.getUsername().equals(username)) {
            user.setUsername(username);
            user = userService.save(user);
        }

        return user;
    }

    /**
     * Updating chat info (name, access level)
     *
     * @param chatFrom telegram Chat.
     * @return Chat entity.
     */
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
            chat = new Chat()
                    .setChatId(chatId)
                    .setName(chatName)
                    .setAccessLevel(AccessLevel.NEWCOMER.getValue());
            chat = chatService.save(chat);
        }

        else if (!chat.getName().equals(chatName)) {
            chat.setName(chatName);
            chat = chatService.save(chat);
        }

        return chat;
    }

    /**
     * Updating user stats by his message.
     *
     * @param chat Chat entity.
     * @param user User entity.
     * @param message telegram Message.
     */
    private void updateUserStats(Chat chat, User user, Message message) {
        UserStats userStats = get(chat, user);
        if (userStats == null) {
            LastMessage lastMessage = new LastMessage();

            userStats = new UserStats()
                    .setChat(chat)
                    .setUser(user)
                    .setNumberOfMessages(0)
                    .setNumberOfAllMessages(0L)
                    .setNumberOfStickers(0)
                    .setNumberOfAllStickers(0L)
                    .setNumberOfPhotos(0)
                    .setNumberOfAllPhotos(0L)
                    .setNumberOfAnimations(0)
                    .setNumberOfAllAnimations(0L)
                    .setNumberOfAudio(0)
                    .setNumberOfAllAudio(0L)
                    .setNumberOfDocuments(0)
                    .setNumberOfAllDocuments(0L)
                    .setNumberOfVideos(0)
                    .setNumberOfAllVideos(0L)
                    .setNumberOfVideoNotes(0)
                    .setNumberOfAllVideoNotes(0L)
                    .setNumberOfVoices(0)
                    .setNumberOfAllVoices(0L)
                    .setNumberOfCommands(0)
                    .setNumberOfAllCommands(0L)
                    .setLastMessage(lastMessageService.update(lastMessage, message))
                    .setNumberOfKarma(0)
                    .setNumberOfAllKarma(0L)
                    .setNumberOfGoodness(0)
                    .setNumberOfAllGoodness(0L)
                    .setNumberOfWickedness(0)
                    .setNumberOfAllWickedness(0L);
        }

        if (message.hasText()) {
            userStats.setNumberOfMessages(userStats.getNumberOfMessages() + 1)
                    .setNumberOfAllMessages(userStats.getNumberOfAllMessages() + 1);
            LastMessage lastMessage = userStats.getLastMessage();
            userStats.setLastMessage(lastMessageService.update(lastMessage, message));
        }
        else if (message.hasSticker()) {
            userStats.setNumberOfStickers(userStats.getNumberOfStickers() + 1)
                    .setNumberOfAllStickers(userStats.getNumberOfAllStickers() + 1);
        }
        else if (message.hasPhoto()) {
            userStats.setNumberOfPhotos(userStats.getNumberOfPhotos() + 1)
                    .setNumberOfAllPhotos(userStats.getNumberOfAllPhotos() + 1);
        }
        else if (message.hasAnimation()) {
            userStats.setNumberOfAnimations(userStats.getNumberOfAnimations() + 1)
                    .setNumberOfAllAnimations(userStats.getNumberOfAllAnimations() + 1);
        }
        else if (message.hasAudio()) {
            userStats.setNumberOfAudio(userStats.getNumberOfAudio() + 1)
                    .setNumberOfAllAudio(userStats.getNumberOfAllAudio() + 1);
        }
        else if (message.hasDocument()) {
            userStats.setNumberOfDocuments(userStats.getNumberOfDocuments() + 1)
                    .setNumberOfAllDocuments(userStats.getNumberOfAllDocuments() + 1);
        }
        else if (message.hasVideo()) {
            userStats.setNumberOfVideos(userStats.getNumberOfVideos() + 1)
                    .setNumberOfAllVideos(userStats.getNumberOfAllVideos() + 1);
        }
        else if (message.hasVideoNote()) {
            userStats.setNumberOfVideoNotes(userStats.getNumberOfVideoNotes() + 1)
                    .setNumberOfAllVideoNotes(userStats.getNumberOfAllVideoNotes() + 1);
        }
        else if (message.hasVoice()) {
            userStats.setNumberOfVoices(userStats.getNumberOfVoices() + 1)
                    .setNumberOfAllVoices(userStats.getNumberOfAllVoices() + 1);
        }

        save(userStats);
    }
}
