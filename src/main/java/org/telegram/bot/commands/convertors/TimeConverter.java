package org.telegram.bot.commands.convertors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.telegram.bot.services.InternalizationService;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimeConverter implements UnitsConverter {

    private final InternalizationService internalizationService;

    private static final String CAPTION = "${command.converter.time.caption}";
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final Map<String, Set<String>> unitNameAbbreviaturesMap = new ConcurrentHashMap<>();

    @PostConstruct
    private void postConstruct() {
        unitNameAbbreviaturesMap.put("FEMTOSECOND", internalizationService.getAllTranslations("command.converter.time.fs"));
        unitNameAbbreviaturesMap.put("PICOSECOND", internalizationService.getAllTranslations("command.converter.time.ps"));
        unitNameAbbreviaturesMap.put("NANOSECOND", internalizationService.getAllTranslations("command.converter.time.ns"));
        unitNameAbbreviaturesMap.put("MICROSECOND", internalizationService.getAllTranslations("command.converter.time.mks"));
        unitNameAbbreviaturesMap.put("MILLISECOND", internalizationService.getAllTranslations("command.converter.time.ms"));
        unitNameAbbreviaturesMap.put("CENTISECOND", internalizationService.getAllTranslations("command.converter.time.cs"));
        unitNameAbbreviaturesMap.put("SECOND", internalizationService.getAllTranslations("command.converter.time.s"));
        unitNameAbbreviaturesMap.put("MINUTE", internalizationService.getAllTranslations("command.converter.time.m"));
        unitNameAbbreviaturesMap.put("HOUR", internalizationService.getAllTranslations("command.converter.time.h"));
        unitNameAbbreviaturesMap.put("DAY", internalizationService.getAllTranslations("command.converter.time.d"));
        unitNameAbbreviaturesMap.put("YEAR", internalizationService.getAllTranslations("command.converter.time.y"));
        unitNameAbbreviaturesMap.put("CENTURY", internalizationService.getAllTranslations("command.converter.time.c"));
    }

    @Override
    public String convert(BigDecimal value, String from, String to) {
        TimeUnit fromUnit = getByAbbreviate(from);
        TimeUnit toUnit = getByAbbreviate(to);
        if (fromUnit == null || toUnit == null) {
            return null;
        }

        Pair<BigDecimal, String> resultPair = calculate(value, fromUnit.getMultiplier(), toUnit.getMultiplier(), ROUNDING_MODE);

        BigDecimal result = resultPair.getFirst();
        String help = resultPair.getSecond();

        return bigDecimalToString(value) + " " + fromUnit.getName() + " = <b>"
                + bigDecimalToString(result) + " " + toUnit.getName() + "</b>\n( " + help + ")";
    }

    @Override
    public String getInfo() {
        String availableUnits = Arrays.stream(TimeUnit.values())
                .map(unit -> unit.getName() + " — " + unit.getName())
                .collect(Collectors.joining("\n"));
        return "<b>" + CAPTION + "</b>\n" + availableUnits + "\n";
    }

    private TimeUnit getByAbbreviate(String abbr) {
        if (abbr == null) {
            return null;
        }

        for (Map.Entry<String, Set<String>> entry : unitNameAbbreviaturesMap.entrySet()) {
            String unitName = entry.getKey();
            Set<String> abbrList = entry.getValue();

            if (abbrList.contains(abbr)) {
                return TimeUnit.valueOf(unitName.toUpperCase());
            }
        }

        return null;
    }

    @RequiredArgsConstructor
    @Getter
    private enum TimeUnit {
        FEMTOSECOND("${command.converter.time.femtosecond}", new BigDecimal("1").setScale(3, ROUNDING_MODE)),
        PICOSECOND("${command.converter.time.picosecond}", new BigDecimal("1000").setScale(3, ROUNDING_MODE)),
        NANOSECOND("${command.converter.time.nanosecond}", new BigDecimal("1000000").setScale(3, ROUNDING_MODE)),
        MICROSECOND("${command.converter.time.microsecond}", new BigDecimal("1000000000").setScale(3, ROUNDING_MODE)),
        MILLISECOND("${command.converter.time.millisecond}", new BigDecimal("1000000000000").setScale(3, ROUNDING_MODE)),
        CENTISECOND("${command.converter.time.centisecond}", new BigDecimal("10000000000000").setScale(3, ROUNDING_MODE)),
        SECOND("${command.converter.time.second}", new BigDecimal("1000000000000000").setScale(3, ROUNDING_MODE)),
        MINUTE("${command.converter.time.minute}", new BigDecimal("60000000000000000").setScale(3, ROUNDING_MODE)),
        HOUR("${command.converter.time.hour}", new BigDecimal("3600000000000000000").setScale(3, ROUNDING_MODE)),
        DAY("${command.converter.time.day}", new BigDecimal("86400000000000000000").setScale(3, ROUNDING_MODE)),
        YEAR("${command.converter.time.year}", new BigDecimal("31536000000000000000000").setScale(3, ROUNDING_MODE)),
        CENTURY("${command.converter.time.century}", new BigDecimal("3153600000000000000000000").setScale(3, ROUNDING_MODE)),
        ;

        private final String name;
        private final BigDecimal multiplier;
    }

}
