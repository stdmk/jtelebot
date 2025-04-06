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
public class TimeUnit implements Unit {

    private final InternationalizationService internationalizationService;

    private static final String CAPTION = "${command.converter.time.caption}";

    private final Map<String, MetricUnit> metricUnitAbbreviaturesSetMap = new ConcurrentHashMap<>();

    @PostConstruct
    private void postConstruct() {
        MetricUnit femtosecond = new MetricUnit("${command.converter.time.femtosecond}", new BigDecimal("1").setScale(3, ROUNDING_MODE));
        MetricUnit picosecond = new MetricUnit("${command.converter.time.picosecond}", new BigDecimal("1000").setScale(3, ROUNDING_MODE));
        MetricUnit nanosecond = new MetricUnit("${command.converter.time.nanosecond}", new BigDecimal("1000000").setScale(3, ROUNDING_MODE));
        MetricUnit microsecond = new MetricUnit("${command.converter.time.microsecond}", new BigDecimal("1000000000").setScale(3, ROUNDING_MODE));
        MetricUnit millisecond = new MetricUnit("${command.converter.time.millisecond}", new BigDecimal("1000000000000").setScale(3, ROUNDING_MODE));
        MetricUnit centisecond = new MetricUnit("${command.converter.time.centisecond}", new BigDecimal("10000000000000").setScale(3, ROUNDING_MODE));
        MetricUnit second = new MetricUnit("${command.converter.time.second}", new BigDecimal("1000000000000000").setScale(3, ROUNDING_MODE));
        MetricUnit minute = new MetricUnit("${command.converter.time.minute}", new BigDecimal("60000000000000000").setScale(3, ROUNDING_MODE));
        MetricUnit hour = new MetricUnit("${command.converter.time.hour}", new BigDecimal("3600000000000000000").setScale(3, ROUNDING_MODE));
        MetricUnit day = new MetricUnit("${command.converter.time.day}", new BigDecimal("86400000000000000000").setScale(3, ROUNDING_MODE));
        MetricUnit year = new MetricUnit("${command.converter.time.year}", new BigDecimal("31536000000000000000000").setScale(3, ROUNDING_MODE));
        MetricUnit century = new MetricUnit("${command.converter.time.century}", new BigDecimal("3153600000000000000000000").setScale(3, ROUNDING_MODE));

        internationalizationService.getAllTranslations("command.converter.time.fs").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, femtosecond));
        internationalizationService.getAllTranslations("command.converter.time.ps").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, picosecond));
        internationalizationService.getAllTranslations("command.converter.time.ns").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, nanosecond));
        internationalizationService.getAllTranslations("command.converter.time.mks").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, microsecond));
        internationalizationService.getAllTranslations("command.converter.time.ms").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, millisecond));
        internationalizationService.getAllTranslations("command.converter.time.cs").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, centisecond));
        internationalizationService.getAllTranslations("command.converter.time.s").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, second));
        internationalizationService.getAllTranslations("command.converter.time.m").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, minute));
        internationalizationService.getAllTranslations("command.converter.time.h").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, hour));
        internationalizationService.getAllTranslations("command.converter.time.d").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, day));
        internationalizationService.getAllTranslations("command.converter.time.y").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, year));
        internationalizationService.getAllTranslations("command.converter.time.c").forEach(word -> metricUnitAbbreviaturesSetMap.put(word, century));
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
