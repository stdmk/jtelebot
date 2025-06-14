package org.telegram.bot.repositories.calories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.UserCaloriesTarget;

public interface UserCaloriesTargetRepository extends JpaRepository<UserCaloriesTarget, Long> {
    UserCaloriesTarget findByUser(User user);
}
