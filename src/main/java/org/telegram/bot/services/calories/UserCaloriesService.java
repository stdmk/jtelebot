package org.telegram.bot.services.calories;

import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.Product;
import org.telegram.bot.domain.entities.calories.UserCalories;

import java.time.ZoneId;

public interface UserCaloriesService {
    void addCalories(User user, ZoneId zoneId, Product product, double grams);
    UserCalories get(User user, ZoneId zoneId);
}
