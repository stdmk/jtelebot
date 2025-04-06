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
class LengthUnitTest {

    @Mock
    private InternationalizationService internationalizationService;

    @InjectMocks
    private LengthUnit lengthUnit;

    @Test
    void getCaptionTest() {
        final String expected = "${command.converter.length.caption}";
        String actual = lengthUnit.getCaption();
        assertEquals(expected, actual);
    }

    @Test
    void getMetricUnitAbbreviaturesSetMapTest() {
        UnitUtils.addLengthUnitTranslations(internationalizationService);
        ReflectionTestUtils.invokeMethod(lengthUnit, "postConstruct");

        Map<String, MetricUnit> metricUnitAbbreviaturesSetMap = lengthUnit.getMetricUnitAbbreviaturesSetMap();

        assertNotNull(metricUnitAbbreviaturesSetMap);
        assertEquals(14, metricUnitAbbreviaturesSetMap.size());

        assertEquals(new BigDecimal("1").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("fm").getMultiplier());
        assertEquals(new BigDecimal("1000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("pm").getMultiplier());
        assertEquals(new BigDecimal("1000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("nm").getMultiplier());
        assertEquals(new BigDecimal("1000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("mk").getMultiplier());
        assertEquals(new BigDecimal("1000000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("mm").getMultiplier());
        assertEquals(new BigDecimal("10000000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("cm").getMultiplier());
        assertEquals(new BigDecimal("100000000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("dm").getMultiplier());
        assertEquals(new BigDecimal("1000000000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("m").getMultiplier());
        assertEquals(new BigDecimal("1000000000000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("km").getMultiplier());
        assertEquals(new BigDecimal("1609340000000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("mi").getMultiplier());
        assertEquals(new BigDecimal("914400000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("yd").getMultiplier());
        assertEquals(new BigDecimal("304800000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("ft").getMultiplier());
        assertEquals(new BigDecimal("25400000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("inch").getMultiplier());
        assertEquals(new BigDecimal("1852000000000000000").setScale(3, ROUNDING_MODE), metricUnitAbbreviaturesSetMap.get("mn").getMultiplier());
    }

}