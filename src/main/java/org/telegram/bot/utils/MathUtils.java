package org.telegram.bot.utils;

public class MathUtils {
    public static Integer getRandomInRange(Integer from, Integer to) {
        return from + (int) (Math.random() * to);
    }

    public static Long getRandomInRange(Long from, Long to) {
        return from + (long) (Math.random() * to);
    }
}
