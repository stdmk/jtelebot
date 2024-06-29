package org.telegram.bot.commands.convertors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class TimeConverterTest {

    @InjectMocks
    private TimeConverter converter;

    @Test
    void getInfoTest() {
        final String expectedInfo = """
                <b>${command.converter.time.caption}</b>
                ${command.converter.time.femtosecond} — ${command.converter.time.femtosecond}
                ${command.converter.time.picosecond} — ${command.converter.time.picosecond}
                ${command.converter.time.nanosecond} — ${command.converter.time.nanosecond}
                ${command.converter.time.microsecond} — ${command.converter.time.microsecond}
                ${command.converter.time.millisecond} — ${command.converter.time.millisecond}
                ${command.converter.time.centisecond} — ${command.converter.time.centisecond}
                ${command.converter.time.second} — ${command.converter.time.second}
                ${command.converter.time.minute} — ${command.converter.time.minute}
                ${command.converter.time.hour} — ${command.converter.time.hour}
                ${command.converter.time.day} — ${command.converter.time.day}
                ${command.converter.time.year} — ${command.converter.time.year}
                ${command.converter.time.century} — ${command.converter.time.century}
                """;
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
        ReflectionTestUtils.setField(converter, "unitNameAbbreviaturesMap", getUnitNameAbbreviaturesMap());
        String actualResult = converter.convert(value, from, to);
        assertEquals(expectedResult, actualResult);
    }

    private static Stream<Arguments> provideValues() {
        return Stream.of(
                Arguments.of(BigDecimal.ONE, "s", "mks", "1 ${command.converter.time.second} = <b>1000000 ${command.converter.time.microsecond}</b>\n( * 1000000)"),
                Arguments.of(BigDecimal.ONE, "mks", "s", "1 ${command.converter.time.microsecond} = <b>0.000001 ${command.converter.time.second}</b>\n( / 1000000)"),
                Arguments.of(BigDecimal.ONE, "s", "s", "1 ${command.converter.time.second} = <b>1 ${command.converter.time.second}</b>\n( * 1)")
        );
    }

    private Map<String, Set<String>> getUnitNameAbbreviaturesMap() {
        Map<String, Set<String>> unitNameAbbreviaturesMap = new HashMap<>();

        unitNameAbbreviaturesMap.put("FEMTOSECOND", Set.of("fs"));
        unitNameAbbreviaturesMap.put("PICOSECOND", Set.of("ps"));
        unitNameAbbreviaturesMap.put("NANOSECOND", Set.of("ns"));
        unitNameAbbreviaturesMap.put("MICROSECOND", Set.of("mks"));
        unitNameAbbreviaturesMap.put("MILLISECOND", Set.of("ms"));
        unitNameAbbreviaturesMap.put("CENTISECOND", Set.of("cs"));
        unitNameAbbreviaturesMap.put("SECOND", Set.of("s"));
        unitNameAbbreviaturesMap.put("MINUTE", Set.of("m"));
        unitNameAbbreviaturesMap.put("HOUR", Set.of("h"));
        unitNameAbbreviaturesMap.put("DAY", Set.of("d"));
        unitNameAbbreviaturesMap.put("YEAR", Set.of("y"));
        unitNameAbbreviaturesMap.put("CENTURY", Set.of("c"));

        return unitNameAbbreviaturesMap;
    }

}