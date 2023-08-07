package org.telegram.bot.domain.commands.convertors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class LengthConverter implements UnitsConverter {
    
    private static final String CAPTION = "Конвертер длин";
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    @Override
    public String convert(BigDecimal value, String from, String to) {
        LengthUnit fromUnit = LengthUnit.getByName(from);
        LengthUnit toUnit = LengthUnit.getByName(to);
        if (fromUnit == null || toUnit == null) {
            return null;
        }

        Pair<BigDecimal, String> resultPair = calculate(value, fromUnit.getMultiplier(), toUnit.getMultiplier(), ROUNDING_MODE);
        BigDecimal result = resultPair.getFirst();
        String help = resultPair.getSecond();

        return bigDecimalToString(value) + " " + fromUnit.getRuSign() + " = <b>"
                + bigDecimalToString(result) + " " + toUnit.getRuSign() + "</b>\n( " + help + ")";
    }

    @Override
    public String getInfo() {
        String availableUnits = Arrays.stream(LengthUnit.values())
                .map(unit -> unit.getName() + " — " + unit.getRuSign())
                .collect(Collectors.joining("\n"));
        return "<b>" + CAPTION + "</b>\n" + availableUnits + "\n";
    }

    @RequiredArgsConstructor
    @Getter
    private enum LengthUnit {
        FEMTOMETER("Фемтометр", new BigDecimal("1").setScale(3, ROUNDING_MODE), "фм", "fm"),
        PICOMETER("Пикометр", new BigDecimal("1000").setScale(3, ROUNDING_MODE), "пм", "pm"),
        NANOMETER("Нанометр", new BigDecimal("1000000").setScale(3, ROUNDING_MODE), "нм", "nm"),
        MICROMETER("Микрометр", new BigDecimal("1000000000").setScale(3, ROUNDING_MODE), "мк", "mk"),
        MILLIMETER("Миллиметр", new BigDecimal("1000000000000").setScale(3, ROUNDING_MODE), "мм", "mm"),
        CENTIMETER("Сантиметр", new BigDecimal("10000000000000").setScale(3, ROUNDING_MODE), "см", "cm"),
        DECIMETER("Дециметр", new BigDecimal("100000000000000").setScale(3, ROUNDING_MODE), "дм", "dm"),
        METER("Метр", new BigDecimal("1000000000000000").setScale(3, ROUNDING_MODE), "м", "m"),
        KILOMETER("Километр", new BigDecimal("1000000000000000000").setScale(3, ROUNDING_MODE), "км", "km"),
        MILE("Миля", new BigDecimal("1609340000000000000").setScale(3, ROUNDING_MODE), "миля", "mi"),
        YARD("Ярд", new BigDecimal("914400000000000").setScale(3, ROUNDING_MODE), "ярд", "yd"),
        FOOR("Фут", new BigDecimal("304800000000000").setScale(3, ROUNDING_MODE), "фут", "ft"),
        INCH("Дюйм", new BigDecimal("25400000000000").setScale(3, ROUNDING_MODE), "дюйм", "ft"),
        NAUTICAL_MILE("Морская миля", new BigDecimal("1852000000000000000").setScale(3, ROUNDING_MODE), "ммиля", "mn"),

        ;

        private final String name;
        private final BigDecimal multiplier;
        private final String ruSign;
        private final String enSign;

        @Nullable
        public static LengthUnit getByName(String name) {
            if (name == null) {
                return null;
            }

            return Arrays.stream(LengthUnit.values())
                    .filter(unit -> unit.getRuSign().equals(name) || unit.getEnSign().equals(name))
                    .findFirst()
                    .orElse(null);
        }

    }
}
