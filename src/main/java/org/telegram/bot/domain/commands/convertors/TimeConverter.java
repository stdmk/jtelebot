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
public class TimeConverter implements UnitsConverter {

    private static final String CAPTION = "Конвертер времени";
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    @Override
    public String convert(BigDecimal value, String from, String to) {
        TimeUnit fromUnit = TimeUnit.getByName(from);
        TimeUnit toUnit = TimeUnit.getByName(to);
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
        String availableUnits = Arrays.stream(TimeUnit.values())
                .map(unit -> unit.getName() + " — " + unit.getRuSign())
                .collect(Collectors.joining("\n"));
        return "<b>" + CAPTION + "</b>\n" + availableUnits + "\n";
    }

    @RequiredArgsConstructor
    @Getter
    private enum TimeUnit {
        FEMTOSECOND("Фемтосекунда", new BigDecimal("1").setScale(3, ROUNDING_MODE), "фс", "fs"),
        PICOSECOND("Пикосекунда", new BigDecimal("1000").setScale(3, ROUNDING_MODE), "пс", "ps"),
        NANOSECOND("Наносекунда", new BigDecimal("1000000").setScale(3, ROUNDING_MODE), "нс", "ns"),
        MICROSECOND("Микросекунда", new BigDecimal("1000000000").setScale(3, ROUNDING_MODE), "мкс", "mks"),
        MILLISECOND("Миллисекунда", new BigDecimal("1000000000000").setScale(3, ROUNDING_MODE), "мс", "ms"),
        CENTISECOND("Сантисекунда", new BigDecimal("10000000000000").setScale(3, ROUNDING_MODE), "сс", "cs"),
        SECOND("Секунда", new BigDecimal("1000000000000000").setScale(3, ROUNDING_MODE), "с", "s"),
        MINUTE("Минута", new BigDecimal("60000000000000000").setScale(3, ROUNDING_MODE), "м", "m"),
        HOUR("Час", new BigDecimal("3600000000000000000").setScale(3, ROUNDING_MODE), "ч", "h"),
        DAY("Сутки", new BigDecimal("86400000000000000000").setScale(3, ROUNDING_MODE), "д", "d"),
        YEAR("Год", new BigDecimal("31536000000000000000000").setScale(3, ROUNDING_MODE), "г", "y"),
        CENTURY("Век", new BigDecimal("3153600000000000000000000").setScale(3, ROUNDING_MODE), "в", "c"),
        ;

        private final String name;
        private final BigDecimal multiplier;
        private final String ruSign;
        private final String enSign;

        @Nullable
        public static TimeUnit getByName(String name) {
            if (name == null) {
                return null;
            }

            return Arrays.stream(TimeUnit.values())
                    .filter(unit -> unit.getRuSign().equals(name) || unit.getEnSign().equals(name))
                    .findFirst()
                    .orElse(null);
        }

    }

}
