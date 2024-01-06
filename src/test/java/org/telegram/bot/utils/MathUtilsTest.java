package org.telegram.bot.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MathUtilsTest {

    @Test
    void getRandomInRangeIntTest() {
        int from = 1;
        int to = 1000;

        List<Integer> results = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            results.add(MathUtils.getRandomInRange(from, to));
        }

        assertFalse(results.stream().anyMatch(integer -> integer < from || integer > to));
    }

    @Test
    void getRandomInRangeLongTest() {
        long from = 1;
        long to = 1000;

        List<Long> results = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            results.add(MathUtils.getRandomInRange(from, to));
        }

        assertFalse(results.stream().anyMatch(integer -> integer < from || integer > to));
    }

    @Test
    void getPercentValueTest() {
        final String expected = "50%";
        String actual = MathUtils.getPercentValue(50L, 100);
        assertEquals(expected, actual);
    }

}