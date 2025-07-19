package org.telegram.bot.mapper.caloric;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.Calories;
import org.telegram.bot.domain.entities.calories.Activity;
import org.telegram.bot.domain.entities.calories.EatenProduct;
import org.telegram.bot.domain.entities.calories.Product;

import java.util.Collection;

@Component
public class CaloricMapper {

    public Calories sum(Collection<Calories> caloriesList) {
        double caloric = 0;
        double proteins = 0;
        double fats = 0;
        double carbs = 0;

        for (Calories calories : caloriesList) {
            caloric = caloric + calories.getCaloric();
            proteins = proteins + calories.getProteins();
            fats = fats + calories.getFats();
            carbs = carbs + calories.getCarbs();
        }

        return new Calories(proteins, fats, carbs, caloric);
    }

    public Calories toCalories(EatenProduct eatenProduct) {
        return toCalories(eatenProduct.getProduct(), eatenProduct.getGrams());
    }

    public Calories toCalories(Activity activity) {
        return new Calories(0, 0, 0, activity.getCalories());
    }

    public Calories toCalories(Product product, double grams) {
        double proteins = product.getProteins() == 0D ? 0D : product.getProteins() / 100 * grams;
        double fats = product.getFats() == 0D ? 0D : product.getFats() / 100 * grams;
        double carbs = product.getCarbs() == 0D ? 0D : product.getCarbs() / 100 * grams;
        double caloric = product.getCaloric() == 0D ? 0D : product.getCaloric() / 100 * grams;

        return new Calories(proteins, fats, carbs, caloric);
    }

    public double toCaloric(double proteins, double fats, double carbs) {
        return proteins * 4 + fats * 9 + carbs * 4;
    }

}
