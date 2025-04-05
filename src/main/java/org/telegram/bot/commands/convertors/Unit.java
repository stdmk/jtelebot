package org.telegram.bot.commands.convertors;

import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;

public interface Unit {
    RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    String getCaption();
    Map<MetricUnit, Set<String>> getMetricUnitAbbreviaturesSetMap();
}
