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
import java.util.ArrayList;

@RequiredArgsConstructor
@Service
public class UserCaloriesServiceImpl implements UserCaloriesService {

    private final UserCaloriesRepository userCaloriesRepository;
    private final EatenProductService eatenProductService;

    @Override
    public void addCalories(User user, LocalDateTime dateTime, Product product, double grams) {
        UserCalories userCalories = this.get(user, dateTime.toLocalDate());

        EatenProduct eatenProduct = eatenProductService.save(new EatenProduct()
                .setProduct(product)
                .setGrams(grams)
                .setDateTime(dateTime)
                .setUserCalories(userCalories));
        userCalories.getEatenProducts().add(eatenProduct);

        userCaloriesRepository.save(userCalories);
    }

    @Override
    public UserCalories get(User user, LocalDate date) {
        UserCalories userCalories = userCaloriesRepository.getByUserAndDate(user, date);

        if (userCalories == null) {
            userCalories = userCaloriesRepository.save(new UserCalories()
                    .setUser(user)
                    .setDate(date)
                    .setEatenProducts(new ArrayList<>()));
        }

        return userCalories;
    }

}
