package org.telegram.bot.services.calories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.EatenProduct;
import org.telegram.bot.domain.entities.calories.Product;
import org.telegram.bot.domain.entities.calories.UserCalories;
import org.telegram.bot.repositories.calories.UserCaloriesRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

@RequiredArgsConstructor
@Service
public class UserCaloriesServiceImpl implements UserCaloriesService {

    private final UserCaloriesRepository userCaloriesRepository;
    private final EatenProductService eatenProductService;

    @Override
    public void addCalories(User user, ZoneId zoneId, Product product, double grams) {
        UserCalories userCalories = this.get(user, zoneId);

        EatenProduct eatenProduct = eatenProductService.save(new EatenProduct()
                .setProduct(product)
                .setGrams(grams)
                .setDateTime(LocalDateTime.now(zoneId))
                .setUserCalories(userCalories));
        userCalories.getEatenProducts().add(eatenProduct);

        userCaloriesRepository.save(userCalories);
    }

    @Override
    public UserCalories get(User user, ZoneId zoneId) {
        UserCalories userCalories = userCaloriesRepository.getByUserAndDate(user, LocalDate.now(zoneId));

        if (userCalories == null) {
            userCalories = userCaloriesRepository.save(new UserCalories()
                    .setUser(user)
                    .setDate(LocalDate.now(zoneId))
                    .setEatenProducts(new ArrayList<>()));
        }

        return userCalories;
    }

}
