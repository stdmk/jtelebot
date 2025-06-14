package org.telegram.bot.services.calories;

import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.UserCaloriesTarget;

public interface UserCaloriesTargetService {
    UserCaloriesTarget get(User user);
    void save(UserCaloriesTarget userCaloriesTarget);
}
