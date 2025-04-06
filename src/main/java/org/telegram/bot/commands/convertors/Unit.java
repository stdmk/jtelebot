package org.telegram.bot.commands.convertors;

import java.math.RoundingMode;
import java.util.Map;

public interface Unit {
    RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    String getCaption();
    Map<String, MetricUnit> getMetricUnitAbbreviaturesSetMap();
}
