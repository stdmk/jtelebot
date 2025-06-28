package org.telegram.bot.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Calories {
    private double proteins;
    private double fats;
    private double carbs;
    private double caloric;

    public void addCalories(Calories calories) {
        this.proteins = this.proteins + calories.getProteins();
        this.fats = this.fats + calories.getFats();
        this.carbs = this.carbs + calories.getCarbs();
        this.caloric = this.caloric + calories.getCaloric();
    }
}
