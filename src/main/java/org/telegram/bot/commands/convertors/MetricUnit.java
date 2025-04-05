package org.telegram.bot.commands.convertors;

import lombok.Getter;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Getter
public class MetricUnit {
    String name;
    BigDecimal multiplier;
}
