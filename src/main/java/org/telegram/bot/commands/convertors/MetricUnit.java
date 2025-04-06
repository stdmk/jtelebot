package org.telegram.bot.commands.convertors;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class MetricUnit {
    String name;
    BigDecimal multiplier;
}
