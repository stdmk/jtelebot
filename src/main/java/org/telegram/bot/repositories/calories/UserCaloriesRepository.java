package org.telegram.bot.repositories.calories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.UserCalories;

import java.time.LocalDate;

public interface UserCaloriesRepository extends JpaRepository<UserCalories, Long> {
    UserCalories getByUserAndDate(User user, LocalDate date);
}
