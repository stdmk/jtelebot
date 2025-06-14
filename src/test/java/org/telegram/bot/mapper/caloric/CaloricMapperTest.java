package org.telegram.bot.mapper.caloric;

import org.junit.jupiter.api.Test;
import org.telegram.bot.domain.Calories;
import org.telegram.bot.domain.entities.calories.EatenProduct;
import org.telegram.bot.domain.entities.calories.Product;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaloricMapperTest {

    private final CaloricMapper caloricMapper = new CaloricMapper();

    @Test
    void sumTest() {
        final double expectedProteins = 6;
        final double expectedFats = 9;
        final double expectedCarbs = 12;
        final double expectedCaloric = 15;

        List<Calories> caloriesList = List.of(
                new Calories(1, 2, 3, 4),
                new Calories(2, 3, 4, 5),
                new Calories(3, 4, 5, 6));

        Calories calories = caloricMapper.sum(caloriesList);

        assertEquals(expectedProteins, calories.getProteins());
        assertEquals(expectedFats, calories.getFats());
        assertEquals(expectedCarbs, calories.getCarbs());
        assertEquals(expectedCaloric, calories.getCaloric());
    }

    @Test
    void toCaloriesTest() {
        final double expectedProteins = 10;
        final double expectedFats = 20;
        final double expectedCarbs = 30;
        final double expectedCaloric = 340;

        EatenProduct eatenProduct = new EatenProduct()
                .setGrams(1000D)
                .setProduct(new Product()
                        .setProteins(1D)
                        .setFats(2D)
                        .setCarbs(3D)
                        .setCaloric(34));

        Calories calories = caloricMapper.toCalories(eatenProduct);

        assertEquals(expectedProteins, calories.getProteins());
        assertEquals(expectedFats, calories.getFats());
        assertEquals(expectedCarbs, calories.getCarbs());
        assertEquals(expectedCaloric, calories.getCaloric());
    }

    @Test
    void toCaloriesZeroValuesTest() {
        final double expectedProteins = 0;
        final double expectedFats = 0;
        final double expectedCarbs = 0;
        final double expectedCaloric = 0;

        EatenProduct eatenProduct = new EatenProduct()
                .setGrams(1000D)
                .setProduct(new Product()
                        .setProteins(0D)
                        .setFats(0D)
                        .setCarbs(0D)
                        .setCaloric(0));

        Calories calories = caloricMapper.toCalories(eatenProduct);

        assertEquals(expectedProteins, calories.getProteins());
        assertEquals(expectedFats, calories.getFats());
        assertEquals(expectedCarbs, calories.getCarbs());
        assertEquals(expectedCaloric, calories.getCaloric());
    }

    @Test
    void toCaloricTest() {
        final double expected = 34;
        double actual = caloricMapper.toCaloric(1, 2, 3);
        assertEquals(expected, actual);
    }

}