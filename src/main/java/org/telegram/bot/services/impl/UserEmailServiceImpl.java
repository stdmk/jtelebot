package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserEmail;
import org.telegram.bot.repositories.UserEmailRepository;
import org.telegram.bot.services.UserEmailService;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserEmailServiceImpl implements UserEmailService {

    private final UserEmailRepository userEmailRepository;

    @Override
    public List<UserEmail> getByUsers(List<User> users) {
        return userEmailRepository.findByUserIn(users);
    }

    @Override
    public void remove(UserEmail userEmail) {
        userEmailRepository.delete(userEmail);
    }

    @Override
    public UserEmail save(UserEmail userEmail) {
        return userEmailRepository.save(userEmail);
    }

    @Override
    public UserEmail get(List<String> email) {
        return userEmailRepository.findByEmailIn(email).stream().findFirst().orElse(null);
    }

    @Override
    public UserEmail get(User user) {
        return userEmailRepository.findByUser(user);
    }

}
