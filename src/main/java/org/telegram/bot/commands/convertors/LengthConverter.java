package org.telegram.bot.commands.convertors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.telegram.bot.services.InternationalizationService;

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
public class LengthConverter implements UnitsConverter {

    private final InternationalizationService internationalizationService;
    
    private static final String CAPTION = "${command.converter.length.caption}";
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final Map<String, Set<String>> unitNameAbbreviaturesMap = new ConcurrentHashMap<>();

    @PostConstruct
    private void postConstruct() {
        unitNameAbbreviaturesMap.put("FEMTOMETER", internationalizationService.getAllTranslations("command.converter.length.fm"));
        unitNameAbbreviaturesMap.put("PICOMETER", internationalizationService.getAllTranslations("command.converter.length.pm"));
        unitNameAbbreviaturesMap.put("NANOMETER", internationalizationService.getAllTranslations("command.converter.length.nm"));
        unitNameAbbreviaturesMap.put("MICROMETER", internationalizationService.getAllTranslations("command.converter.length.mk"));
        unitNameAbbreviaturesMap.put("MILLIMETER", internationalizationService.getAllTranslations("command.converter.length.mm"));
        unitNameAbbreviaturesMap.put("CENTIMETER", internationalizationService.getAllTranslations("command.converter.length.cm"));
        unitNameAbbreviaturesMap.put("DECIMETER", internationalizationService.getAllTranslations("command.converter.length.dm"));
        unitNameAbbreviaturesMap.put("METER", internationalizationService.getAllTranslations("command.converter.length.m"));
        unitNameAbbreviaturesMap.put("KILOMETER", internationalizationService.getAllTranslations("command.converter.length.km"));
        unitNameAbbreviaturesMap.put("MILE", internationalizationService.getAllTranslations("command.converter.length.mi"));
        unitNameAbbreviaturesMap.put("YARD", internationalizationService.getAllTranslations("command.converter.length.yd"));
        unitNameAbbreviaturesMap.put("FOOR", internationalizationService.getAllTranslations("command.converter.length.ft"));
        unitNameAbbreviaturesMap.put("INCH", internationalizationService.getAllTranslations("command.converter.length.nch"));
        unitNameAbbreviaturesMap.put("NAUTICAL_MILE", internationalizationService.getAllTranslations("command.converter.length.mn"));
    }

    @Override
    public String convert(BigDecimal value, String from, String to) {
        LengthUnit fromUnit = getByAbbreviate(from);
        LengthUnit toUnit = getByAbbreviate(to);
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
        String availableUnits = Arrays.stream(LengthUnit.values())
                .map(unit -> unit.getName() + " — " + unit.getName())
                .collect(Collectors.joining("\n"));
        return "<b>" + CAPTION + "</b>\n" + availableUnits + "\n";
    }

    private LengthUnit getByAbbreviate(String abbr) {
        if (abbr == null) {
            return null;
        }

        for (Map.Entry<String, Set<String>> entry : unitNameAbbreviaturesMap.entrySet()) {
            String unitName = entry.getKey();
            Set<String> abbrList = entry.getValue();

            if (abbrList.contains(abbr)) {
                return LengthUnit.valueOf(unitName.toUpperCase());
            }
        }

        return null;
    }

    @RequiredArgsConstructor
    @Getter
    private enum LengthUnit {
        FEMTOMETER("command.converter.length.femtometer", new BigDecimal("1").setScale(3, ROUNDING_MODE)),
        PICOMETER("command.converter.length.picometer", new BigDecimal("1000").setScale(3, ROUNDING_MODE)),
        NANOMETER("command.converter.length.nanometer", new BigDecimal("1000000").setScale(3, ROUNDING_MODE)),
        MICROMETER("command.converter.length.micrometer", new BigDecimal("1000000000").setScale(3, ROUNDING_MODE)),
        MILLIMETER("command.converter.length.millimeter", new BigDecimal("1000000000000").setScale(3, ROUNDING_MODE)),
        CENTIMETER("command.converter.length.centimeter", new BigDecimal("10000000000000").setScale(3, ROUNDING_MODE)),
        DECIMETER("command.converter.length.decimeter", new BigDecimal("100000000000000").setScale(3, ROUNDING_MODE)),
        METER("command.converter.length.meter", new BigDecimal("1000000000000000").setScale(3, ROUNDING_MODE)),
        KILOMETER("command.converter.length.kilometer", new BigDecimal("1000000000000000000").setScale(3, ROUNDING_MODE)),
        MILE("command.converter.length.mile", new BigDecimal("1609340000000000000").setScale(3, ROUNDING_MODE)),
        YARD("command.converter.length.yard", new BigDecimal("914400000000000").setScale(3, ROUNDING_MODE)),
        FOOR("command.converter.length.foor", new BigDecimal("304800000000000").setScale(3, ROUNDING_MODE)),
        INCH("command.converter.length.inch", new BigDecimal("25400000000000").setScale(3, ROUNDING_MODE)),
        NAUTICAL_MILE("command.converter.length.nautical_mile", new BigDecimal("1852000000000000000").setScale(3, ROUNDING_MODE)),
        ;

        private final String name;
        private final BigDecimal multiplier;
    }
}
