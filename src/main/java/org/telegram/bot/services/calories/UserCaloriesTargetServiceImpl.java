package org.telegram.bot.services.calories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.UserCaloriesTarget;
import org.telegram.bot.repositories.calories.UserCaloriesTargetRepository;

@RequiredArgsConstructor
@Service
public class UserCaloriesTargetServiceImpl implements UserCaloriesTargetService {

    private final UserCaloriesTargetRepository userCaloriesTargetRepository;

    @Override
    public UserCaloriesTarget get(User user) {
        return userCaloriesTargetRepository.findByUser(user);
    }

    @Override
    public void save(UserCaloriesTarget userCaloriesTarget) {
        userCaloriesTargetRepository.save(userCaloriesTarget);
    }

}
