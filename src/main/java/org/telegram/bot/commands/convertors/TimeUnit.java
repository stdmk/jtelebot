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
public class TimeUnit implements Unit {

    private final InternationalizationService internationalizationService;

    private static final String CAPTION = "${command.converter.time.caption}";

    private final Map<MetricUnit, Set<String>> metricUnitAbbreviaturesSetMap = new ConcurrentHashMap<>();

    @PostConstruct
    private void postConstruct() {
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.time.femtosecond}", new BigDecimal("1").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.time.fs"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.time.picosecond}", new BigDecimal("1000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.time.ps"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.time.nanosecond}", new BigDecimal("1000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.time.ns"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.time.microsecond}", new BigDecimal("1000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.time.mks"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.time.millisecond}", new BigDecimal("1000000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.time.ms"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.time.centisecond}", new BigDecimal("10000000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.time.cs"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.time.second}", new BigDecimal("1000000000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.time.s"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.time.minute}", new BigDecimal("60000000000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.time.m"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.time.hour}", new BigDecimal("3600000000000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.time.h"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.time.day}", new BigDecimal("86400000000000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.time.d"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.time.year}", new BigDecimal("31536000000000000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.time.y"));
        metricUnitAbbreviaturesSetMap.put(new MetricUnit("${command.converter.time.century}", new BigDecimal("3153600000000000000000000").setScale(3, ROUNDING_MODE)), internationalizationService.getAllTranslations("command.converter.time.c"));
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
