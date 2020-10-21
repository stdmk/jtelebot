package org.telegram.bot.utils;

public class MathUtils {
    public static Integer getRandomInRange(Integer from, Integer to) {
        return from + (int) (Math.random() * to);
    }
}
