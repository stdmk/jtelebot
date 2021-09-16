package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.repositories.UserRepository;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.UserService;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ChatService chatService;

    @Override
    public User get(Long userId) {
        log.debug("Request to get User by userId: {} ", userId);
        return userRepository.findByUserId(userId);
    }

    @Override
    public User get(String username) {
        log.debug("Request to get User by username: {} ", username);
        return userRepository.findByUsername(username.replace("@", ""));
    }

    @Override
    public User save(User user) {
        log.debug("Request to save User: {} ", user);
        return userRepository.save(user);
    }

    @Override
    public Integer getUserAccessLevel(Long userId) {
        log.debug("Request to get userLevel for userId {} ", userId);

        User user = get(userId);
        if (user == null) {
            return AccessLevel.NEWCOMER.getValue();
        }

        return user.getAccessLevel();
    }

    @Override
    public AccessLevel getCurrentAccessLevel(Long userId, Long chatId) {
        AccessLevel level = AccessLevel.BANNED;

        Integer userLevel = getUserAccessLevel(userId);
        if (userLevel < 0) {
            return level;
        }
        Integer chatLevel = chatService.getChatAccessLevel(chatId);

        if (userLevel > chatLevel) {
            level = AccessLevel.getUserLevelByValue(userLevel);
        } else {
            level = AccessLevel.getUserLevelByValue(chatLevel);
        }

        return level;
    }

    @Override
    public Boolean isUserHaveAccessForCommand(Integer userAccessLevel, Integer commandAccessLevel) {
        return userAccessLevel >= commandAccessLevel;
    }

}
