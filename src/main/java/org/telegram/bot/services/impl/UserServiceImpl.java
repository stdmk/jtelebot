package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.AccessLevels;
import org.telegram.bot.repositories.UserRepository;
import org.telegram.bot.services.UserService;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    @Override
    public User get(Integer userId) {
        log.debug("Request to get User by userId: {} ", userId);
        return userRepository.findById(userId).orElse(null);
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
    public Integer getUserAccessLevel(Integer userId) {
        log.debug("Request to get userLevel for userId {} ", userId);
        User user = get(userId);
        if (user == null) {
            return AccessLevels.NEWCOMER.getValue();
        }

        return user.getAccessLevel();
    }
}
