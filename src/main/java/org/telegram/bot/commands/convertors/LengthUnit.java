package org.telegram.bot.commands.convertors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.services.InternationalizationService;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class LengthUnit implements Unit {

    private final InternationalizationService internationalizationService;
    
    private static final String CAPTION = "${command.converter.length.caption}";

    private final Map<MetricUnit, Set<String>> metricUnitAbbreviaturesSetMap = new ConcurrentHashMap<>();

    @PostConstruct
    private void postConstruct() {
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.length.femtometer}", new BigDecimal("1").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.length.fm"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.length.picometer}", new BigDecimal("1000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.length.pm"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.length.nanometer}", new BigDecimal("1000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.length.nm"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.length.micrometer}", new BigDecimal("1000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.length.mk"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.length.millimeter}", new BigDecimal("1000000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.length.mm"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.length.centimeter}", new BigDecimal("10000000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.length.cm"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.length.decimeter}", new BigDecimal("100000000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.length.dm"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.length.meter}", new BigDecimal("1000000000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.length.m"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.length.kilometer}", new BigDecimal("1000000000000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.length.km"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.length.mile}", new BigDecimal("1609340000000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.length.mi"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.length.yard}", new BigDecimal("914400000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.length.yd"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.length.foor}", new BigDecimal("304800000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.length.ft"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.length.inch}", new BigDecimal("25400000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.length.nch"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.length.mn}", new BigDecimal("1852000000000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.length.mn"));
    }


    @Override
    public String getCaption() {
        return CAPTION;
    }

    @Override
    public Map<MetricUnit, Set<String>> getMetricUnitAbbreviaturesSetMap() {
        return this.metricUnitAbbreviaturesSetMap;
    }

}
