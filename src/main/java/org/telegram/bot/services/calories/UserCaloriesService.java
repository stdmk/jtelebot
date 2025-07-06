package org.telegram.bot.services.calories;

import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.Product;
import org.telegram.bot.domain.entities.calories.UserCalories;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface UserCaloriesService {
    void addCalories(User user, LocalDateTime dateTime, Product product, double grams);
    UserCalories get(User user, LocalDate date);
}
