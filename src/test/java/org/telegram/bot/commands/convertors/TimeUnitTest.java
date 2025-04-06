package org.telegram.bot.commands.convertors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.UnitUtils;
import org.telegram.bot.services.InternationalizationService;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.telegram.bot.commands.convertors.Unit.ROUNDING_MODE;

@ExtendWith(MockitoExtension.class)
class TimeUnitTest {

    @Mock
    private InternationalizationService internationalizationService;

    @InjectMocks
    private TimeUnit timeUnit;

    @Test
    void getCaptionTest() {
        final String expected = "${command.converter.time.caption}";
        String actual = timeUnit.getCaption();
        assertEquals(expected, actual);
    }

    @Test
    void getMetricUnitAbbreviaturesSetMapTest() {
        UnitUtils.addTimeUnitTranslations(internationalizationService);
        ReflectionTestUtils.invokeMethod(timeUnit, "postConstruct");

        Map<String, MetricUnit> metricUnitAbbreviaturesSetMap = timeUnit.getMetricUnitAbbreviaturesSetMap();

        assertNotNull(metricUnitAbbreviaturesSetMap);
        assertEquals(12, metricUnitAbbreviaturesSetMap.size());

        assertEquals(new BigDecimal("1").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("fs").getMultiplier());
        assertEquals(new BigDecimal("1000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("ps").getMultiplier());
        assertEquals(new BigDecimal("1000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("ns").getMultiplier());
        assertEquals(new BigDecimal("1000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("mks").getMultiplier());
        assertEquals(new BigDecimal("1000000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("ms").getMultiplier());
        assertEquals(new BigDecimal("10000000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("cs").getMultiplier());
        assertEquals(new BigDecimal("1000000000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("s").getMultiplier());
        assertEquals(new BigDecimal("60000000000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("m").getMultiplier());
        assertEquals(new BigDecimal("3600000000000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("h").getMultiplier());
        assertEquals(new BigDecimal("86400000000000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("d").getMultiplier());
        assertEquals(new BigDecimal("31536000000000000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("y").getMultiplier());
        assertEquals(new BigDecimal("3153600000000000000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("c").getMultiplier());
    }

}