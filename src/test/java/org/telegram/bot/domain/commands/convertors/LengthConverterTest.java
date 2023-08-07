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
class LengthConverterTest {

    @InjectMocks
    private LengthConverter converter;

    @Test
    void getInfoTest() {
        final String expectedInfo = "<b>Конвертер длин</b>\n" +
                "Фемтометр — фм\n" +
                "Пикометр — пм\n" +
                "Нанометр — нм\n" +
                "Микрометр — мк\n" +
                "Миллиметр — мм\n" +
                "Сантиметр — см\n" +
                "Дециметр — дм\n" +
                "Метр — м\n" +
                "Километр — км\n" +
                "Миля — миля\n" +
                "Ярд — ярд\n" +
                "Фут — фут\n" +
                "Дюйм — дюйм\n" +
                "Морская миля — ммиля\n";
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
                Arguments.of(BigDecimal.ONE, "км", "мм", "1 км = <b>1000000 мм</b>\n( * 1000000)"),
                Arguments.of(BigDecimal.ONE, "мм", "км", "1 мм = <b>0.000001 км</b>\n( / 1000000)"),
                Arguments.of(BigDecimal.ONE, "мм", "мм", "1 мм = <b>1 мм</b>\n( * 1)"),
                Arguments.of(BigDecimal.ONE, "миля", "м", "1 миля = <b>1609.34 м</b>\n( * 1609.34)"),
                Arguments.of(BigDecimal.ONE, "ярд", "м", "1 ярд = <b>0.9141 м</b>\n( / 1.094)"),
                Arguments.of(BigDecimal.ONE, "фут", "м", "1 фут = <b>0.3048 м</b>\n( / 3.281)"),
                Arguments.of(BigDecimal.ONE, "ммиля", "м", "1 ммиля = <b>1852 м</b>\n( * 1852)"),
                Arguments.of(BigDecimal.ONE, "дюйм", "см", "1 дюйм = <b>2.54 см</b>\n( * 2.54)"),
                Arguments.of(new BigDecimal("1000000000000000000"), "км", "фм", "1000000000000000000 км = <b>1000000000000000000000000000000000000 фм</b>\n( * 1000000000000000000)")
        );
    }

}