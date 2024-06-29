package org.telegram.bot.utils;

import lombok.experimental.UtilityClass;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class MathUtils {

    /**
     * Get random value;
     *
     * @param from inclusive.
     * @param to exclusive.
     * @return random value.
     */
    public static Integer getRandomInRange(int from, int to) {
        if (from == to) {
            return from;
        }

        return ThreadLocalRandom.current().nextInt(from, to);
    }

    /**
     * Get random value;
     *
     * @param from inclusive.
     * @param to exclusive.
     * @return random value.
     */
    public static Long getRandomInRange(long from, long to) {
        if (from == to) {
            return from;
        }

        return ThreadLocalRandom.current().nextLong(from, to);
    }

    public static String getPercentValue(long value, long count) {
        return (int) ((double) value / count * 100) + "%";
    }
}
