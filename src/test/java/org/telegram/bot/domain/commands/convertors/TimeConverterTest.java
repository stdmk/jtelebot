package org.telegram.bot.domain.commands.convertors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TimeConverterTest {

    @InjectMocks
    private TimeConverter converter;

    @Test
    void getInfoTest() {
        final String expectedInfo = "<b>Конвертер времени</b>\n" +
                "Фемтосекунда — фс\n" +
                "Пикосекунда — пс\n" +
                "Наносекунда — нс\n" +
                "Микросекунда — мкс\n" +
                "Миллисекунда — мс\n" +
                "Сантисекунда — сс\n" +
                "Секунда — с\n" +
                "Минута — м\n" +
                "Час — ч\n" +
                "Сутки — д\n" +
                "Год — г\n" +
                "Век — в\n";
        String actualInfo = converter.getInfo();
        assertEquals(expectedInfo, actualInfo);
    }

    @ParameterizedTest
    @MethodSource("provideUnits")
    void convertWithWrongInputTest(String from, String to) {
        assertNull(converter.convert(BigDecimal.ZERO, from, to));
    }

    private static Stream<Arguments> provideUnits() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(null, ""),
                Arguments.of("", null),
                Arguments.of("", ""),
                Arguments.of("см", ""),
                Arguments.of("", "мм")
        );
    }

    @ParameterizedTest
    @MethodSource("provideValues")
    void convertTest(BigDecimal value, String from, String to, String expectedResult) {
        String actualResult = converter.convert(value, from, to);
        assertEquals(expectedResult, actualResult);
    }

    private static Stream<Arguments> provideValues() {
        return Stream.of(
                Arguments.of(BigDecimal.ONE, "с", "мкс", "1 с = <b>1000000 мкс</b>\n( * 1000000)"),
                Arguments.of(BigDecimal.ONE, "мкс", "с", "1 мкс = <b>0.000001 с</b>\n( / 1000000)"),
                Arguments.of(BigDecimal.ONE, "с", "с", "1 с = <b>1 с</b>\n( * 1)")
        );
    }

}