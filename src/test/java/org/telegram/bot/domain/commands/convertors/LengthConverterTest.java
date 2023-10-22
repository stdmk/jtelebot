package org.telegram.bot.domain.commands.convertors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.commands.convertors.LengthConverter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LengthConverterTest {

    @InjectMocks
    private LengthConverter converter;

    @Test
    void getInfoTest() {
        final String expectedInfo = "<b>${command.converter.length.caption}</b>\n" +
                "command.converter.length.femtometer — command.converter.length.femtometer\n" +
                "command.converter.length.picometer — command.converter.length.picometer\n" +
                "command.converter.length.nanometer — command.converter.length.nanometer\n" +
                "command.converter.length.micrometer — command.converter.length.micrometer\n" +
                "command.converter.length.millimeter — command.converter.length.millimeter\n" +
                "command.converter.length.centimeter — command.converter.length.centimeter\n" +
                "command.converter.length.decimeter — command.converter.length.decimeter\n" +
                "command.converter.length.meter — command.converter.length.meter\n" +
                "command.converter.length.kilometer — command.converter.length.kilometer\n" +
                "command.converter.length.mile — command.converter.length.mile\n" +
                "command.converter.length.yard — command.converter.length.yard\n" +
                "command.converter.length.foor — command.converter.length.foor\n" +
                "command.converter.length.inch — command.converter.length.inch\n" +
                "command.converter.length.nautical_mile — command.converter.length.nautical_mile\n";
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
                Arguments.of(BigDecimal.ONE, "km", "mm", "1 command.converter.length.kilometer = <b>1000000 command.converter.length.millimeter</b>\n( * 1000000)"),
                Arguments.of(BigDecimal.ONE, "mm", "km", "1 command.converter.length.millimeter = <b>0.000001 command.converter.length.kilometer</b>\n( / 1000000)"),
                Arguments.of(BigDecimal.ONE, "mm", "mm", "1 command.converter.length.millimeter = <b>1 command.converter.length.millimeter</b>\n( * 1)"),
                Arguments.of(BigDecimal.ONE, "mi", "m", "1 command.converter.length.mile = <b>1609.34 command.converter.length.meter</b>\n( * 1609.34)"),
                Arguments.of(BigDecimal.ONE, "yd", "m", "1 command.converter.length.yard = <b>0.9141 command.converter.length.meter</b>\n( / 1.094)"),
                Arguments.of(BigDecimal.ONE, "ft", "m", "1 command.converter.length.foor = <b>0.3048 command.converter.length.meter</b>\n( / 3.281)"),
                Arguments.of(BigDecimal.ONE, "mn", "m", "1 command.converter.length.nautical_mile = <b>1852 command.converter.length.meter</b>\n( * 1852)"),
                Arguments.of(BigDecimal.ONE, "inch", "cm", "1 command.converter.length.inch = <b>2.54 command.converter.length.centimeter</b>\n( * 2.54)"),
                Arguments.of(new BigDecimal("1000000000000000000"), "km", "fm", "1000000000000000000 command.converter.length.kilometer = <b>1000000000000000000000000000000000000 command.converter.length.femtometer</b>\n( * 1000000000000000000)")
        );
    }

    private Map<String, Set<String>> getUnitNameAbbreviaturesMap() {
        Map<String, Set<String>> unitNameAbbreviaturesMap = new HashMap<>();

        unitNameAbbreviaturesMap.put("FEMTOMETER", Set.of("fm"));
        unitNameAbbreviaturesMap.put("PICOMETER", Set.of("pm"));
        unitNameAbbreviaturesMap.put("NANOMETER", Set.of("nm"));
        unitNameAbbreviaturesMap.put("MICROMETER", Set.of("mk"));
        unitNameAbbreviaturesMap.put("MILLIMETER", Set.of("mm"));
        unitNameAbbreviaturesMap.put("CENTIMETER", Set.of("cm"));
        unitNameAbbreviaturesMap.put("DECIMETER", Set.of("dm"));
        unitNameAbbreviaturesMap.put("METER", Set.of("m"));
        unitNameAbbreviaturesMap.put("KILOMETER", Set.of("km"));
        unitNameAbbreviaturesMap.put("MILE", Set.of("mi"));
        unitNameAbbreviaturesMap.put("YARD", Set.of("yd"));
        unitNameAbbreviaturesMap.put("FOOR", Set.of("ft"));
        unitNameAbbreviaturesMap.put("INCH", Set.of("inch"));
        unitNameAbbreviaturesMap.put("NAUTICAL_MILE", Set.of("mn"));

        return unitNameAbbreviaturesMap;
    }

}