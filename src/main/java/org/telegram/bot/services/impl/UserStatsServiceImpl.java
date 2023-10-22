package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.repositories.UserStatsRepository;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.LastCommandService;
import org.telegram.bot.services.LastMessageService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.telegrambots.meta.api.objects.Message;

import javax.transaction.Transactional;
import java.time.LocalDate;
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
    public void save(List<UserStats> userStatsList) {
        log.debug("Request to save UserStats list {}", userStatsList);
        userStatsRepository.saveAll(userStatsList);
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
    public List<UserStats> getActiveUserStatsListForChat(Chat chat) {
        log.debug("Request to get user stats of chat {}", chat);
        return userStatsRepository.findByChatAndLastMessageDateGreaterThan(chat, LocalDate.now().atStartOfDay());
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
    public void clearMonthlyStats() {
        log.debug("Request to clear monthly stats of users");
        userStatsRepository.saveAll(getAllGroupStats()
                .stream()
                .peek(this::clearUserStatsFields)
                .collect(Collectors.toList()));
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
    public void clearDailyStats() {
        log.debug("Request to clear daily stats of users");
        userStatsRepository.saveAll(getAllGroupStats()
                .stream()
                .peek(this::clearUserStatsPerDayFields)
                .collect(Collectors.toList()));
    }

    private void clearUserStatsPerDayFields(UserStats userStats) {
        userStats.setNumberOfMessagesPerDay(0)
                .setNumberOfPhotosPerDay(0)
                .setNumberOfAnimationsPerDay(0)
                .setNumberOfAudioPerDay(0)
                .setNumberOfDocumentsPerDay(0)
                .setNumberOfVideosPerDay(0)
                .setNumberOfVideoNotesPerDay(0)
                .setNumberOfVoicesPerDay(0)
                .setNumberOfCommandsPerDay(0)
                .setNumberOfStickersPerDay(0)
                .setNumberOfKarmaPerDay(0)
                .setNumberOfGoodnessPerDay(0)
                .setNumberOfWickednessPerDay(0);
    }

    @Override
    public void incrementUserStatsCommands(Chat chat, User user) {
        log.debug("Request to increment users stats commands using");

        UserStats userStats = get(chat, user);
        userStats
                .setNumberOfCommands(userStats.getNumberOfCommands() + 1)
                .setNumberOfCommandsPerDay(userStats.getNumberOfCommandsPerDay() + 1)
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
        if (!chatName.equals(chat.getName())) {
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
                    .setNumberOfMessagesPerDay(0)
                    .setNumberOfAllMessages(0L)
                    .setNumberOfStickers(0)
                    .setNumberOfStickersPerDay(0)
                    .setNumberOfAllStickers(0L)
                    .setNumberOfPhotos(0)
                    .setNumberOfPhotosPerDay(0)
                    .setNumberOfAllPhotos(0L)
                    .setNumberOfAnimations(0)
                    .setNumberOfAnimationsPerDay(0)
                    .setNumberOfAllAnimations(0L)
                    .setNumberOfAudio(0)
                    .setNumberOfAudioPerDay(0)
                    .setNumberOfAllAudio(0L)
                    .setNumberOfDocuments(0)
                    .setNumberOfDocumentsPerDay(0)
                    .setNumberOfAllDocuments(0L)
                    .setNumberOfVideos(0)
                    .setNumberOfVideosPerDay(0)
                    .setNumberOfAllVideos(0L)
                    .setNumberOfVideoNotes(0)
                    .setNumberOfVideoNotesPerDay(0)
                    .setNumberOfAllVideoNotes(0L)
                    .setNumberOfVoices(0)
                    .setNumberOfVoicesPerDay(0)
                    .setNumberOfAllVoices(0L)
                    .setNumberOfCommands(0)
                    .setNumberOfCommandsPerDay(0)
                    .setNumberOfAllCommands(0L)
                    .setLastMessage(lastMessageService.update(lastMessage, message))
                    .setNumberOfKarma(0)
                    .setNumberOfKarmaPerDay(0)
                    .setNumberOfAllKarma(0L)
                    .setNumberOfGoodness(0)
                    .setNumberOfGoodnessPerDay(0)
                    .setNumberOfAllGoodness(0L)
                    .setNumberOfWickedness(0)
                    .setNumberOfWickednessPerDay(0)
                    .setNumberOfAllWickedness(0L);
        }

        if (message.hasText()) {
            userStats.setNumberOfMessages(userStats.getNumberOfMessages() + 1)
                    .setNumberOfMessagesPerDay(userStats.getNumberOfMessagesPerDay() + 1)
                    .setNumberOfAllMessages(userStats.getNumberOfAllMessages() + 1);
        }
        else if (message.hasSticker()) {
            userStats.setNumberOfStickers(userStats.getNumberOfStickers() + 1)
                    .setNumberOfStickersPerDay(userStats.getNumberOfStickersPerDay() + 1)
                    .setNumberOfAllStickers(userStats.getNumberOfAllStickers() + 1);
        }
        else if (message.hasPhoto()) {
            userStats.setNumberOfPhotos(userStats.getNumberOfPhotos() + 1)
                    .setNumberOfPhotosPerDay(userStats.getNumberOfPhotosPerDay() + 1)
                    .setNumberOfAllPhotos(userStats.getNumberOfAllPhotos() + 1);
        }
        else if (message.hasAnimation()) {
            userStats.setNumberOfAnimations(userStats.getNumberOfAnimations() + 1)
                    .setNumberOfAnimationsPerDay(userStats.getNumberOfAnimationsPerDay() + 1)
                    .setNumberOfAllAnimations(userStats.getNumberOfAllAnimations() + 1);
        }
        else if (message.hasAudio()) {
            userStats.setNumberOfAudio(userStats.getNumberOfAudio() + 1)
                    .setNumberOfAudioPerDay(userStats.getNumberOfAudioPerDay() + 1)
                    .setNumberOfAllAudio(userStats.getNumberOfAllAudio() + 1);
        }
        else if (message.hasDocument()) {
            userStats.setNumberOfDocuments(userStats.getNumberOfDocuments() + 1)
                    .setNumberOfDocumentsPerDay(userStats.getNumberOfDocumentsPerDay() + 1)
                    .setNumberOfAllDocuments(userStats.getNumberOfAllDocuments() + 1);
        }
        else if (message.hasVideo()) {
            userStats.setNumberOfVideos(userStats.getNumberOfVideos() + 1)
                    .setNumberOfVideosPerDay(userStats.getNumberOfVideosPerDay() + 1)
                    .setNumberOfAllVideos(userStats.getNumberOfAllVideos() + 1);
        }
        else if (message.hasVideoNote()) {
            userStats.setNumberOfVideoNotes(userStats.getNumberOfVideoNotes() + 1)
                    .setNumberOfVideoNotesPerDay(userStats.getNumberOfVideoNotesPerDay() + 1)
                    .setNumberOfAllVideoNotes(userStats.getNumberOfAllVideoNotes() + 1);
        }
        else if (message.hasVoice()) {
            userStats.setNumberOfVoices(userStats.getNumberOfVoices() + 1)
                    .setNumberOfVoicesPerDay(userStats.getNumberOfVoicesPerDay() + 1)
                    .setNumberOfAllVoices(userStats.getNumberOfAllVoices() + 1);
        }

        userStats.setLastMessage(lastMessageService.update(userStats.getLastMessage(), message));

        save(userStats);
    }
}
