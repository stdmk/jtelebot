package org.telegram.bot.commands.convertors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.services.InternationalizationService;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class LengthUnit implements Unit {

    private final InternationalizationService internationalizationService;
    
    private static final String CAPTION = "${command.converter.length.caption}";

    private final Map<String, MetricUnit> metricUnitAbbreviaturesSetMap = new ConcurrentHashMap<>();

    @PostConstruct
    private void postConstruct() {
        MetricUnit femtometer = new MetricUnit("${command.converter.length.femtometer}", new BigDecimal("1").setScale(3, ROUNDING_MODE));
        MetricUnit picometer = new MetricUnit("${command.converter.length.picometer}", new BigDecimal("1000").setScale(3, ROUNDING_MODE));
        MetricUnit nanometer = new MetricUnit("${command.converter.length.nanometer}", new BigDecimal("1000000").setScale(3, ROUNDING_MODE));
        MetricUnit micrometer = new MetricUnit("${command.converter.length.micrometer}", new BigDecimal("1000000000").setScale(3, ROUNDING_MODE));
        MetricUnit millimeter = new MetricUnit("${command.converter.length.millimeter}", new BigDecimal("1000000000000").setScale(3, ROUNDING_MODE));
        MetricUnit centimeter = new MetricUnit("${command.converter.length.centimeter}", new BigDecimal("10000000000000").setScale(3, ROUNDING_MODE));
        MetricUnit decimeter = new MetricUnit("${command.converter.length.decimeter}", new BigDecimal("100000000000000").setScale(3, ROUNDING_MODE));
        MetricUnit meter = new MetricUnit("${command.converter.length.meter}", new BigDecimal("1000000000000000").setScale(3, ROUNDING_MODE));
        MetricUnit kilometer = new MetricUnit("${command.converter.length.kilometer}", new BigDecimal("1000000000000000000").setScale(3, ROUNDING_MODE));
        MetricUnit mile = new MetricUnit("${command.converter.length.mile}", new BigDecimal("1609340000000000000").setScale(3, ROUNDING_MODE));
        MetricUnit yard = new MetricUnit("${command.converter.length.yard}", new BigDecimal("914400000000000").setScale(3, ROUNDING_MODE));
        MetricUnit foor = new MetricUnit("${command.converter.length.foor}", new BigDecimal("304800000000000").setScale(3, ROUNDING_MODE));
        MetricUnit inch = new MetricUnit("${command.converter.length.inch}", new BigDecimal("25400000000000").setScale(3, ROUNDING_MODE));
        MetricUnit mn = new MetricUnit("${command.converter.length.mn}", new BigDecimal("1852000000000000000").setScale(3, ROUNDING_MODE));

        internationalizationService.getAllTranslations("command.converter.length.fm").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, femtometer));
        internationalizationService.getAllTranslations("command.converter.length.pm").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, picometer));
        internationalizationService.getAllTranslations("command.converter.length.nm").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, nanometer));
        internationalizationService.getAllTranslations("command.converter.length.mk").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, micrometer));
        internationalizationService.getAllTranslations("command.converter.length.mm").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, millimeter));
        internationalizationService.getAllTranslations("command.converter.length.cm").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, centimeter));
        internationalizationService.getAllTranslations("command.converter.length.dm").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, decimeter));
        internationalizationService.getAllTranslations("command.converter.length.m").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, meter));
        internationalizationService.getAllTranslations("command.converter.length.km").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, kilometer));
        internationalizationService.getAllTranslations("command.converter.length.mi").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, mile));
        internationalizationService.getAllTranslations("command.converter.length.yd").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, yard));
        internationalizationService.getAllTranslations("command.converter.length.ft").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, foor));
        internationalizationService.getAllTranslations("command.converter.length.nch").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, inch));
        internationalizationService.getAllTranslations("command.converter.length.mn").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, mn));
    }

    @Override
    public String getCaption() {
        return CAPTION;
    }

    @Override
    public Map<String, MetricUnit> getMetricUnitAbbreviaturesSetMap() {
        return this.metricUnitAbbreviaturesSetMap;
    }

}
