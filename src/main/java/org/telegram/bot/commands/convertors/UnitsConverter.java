package org.telegram.bot.commands.convertors;

import org.springframework.data.util.Pair;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;

public interface UnitsConverter {

    /**
     * Convert value from unit to unit.
     *
     * @param value value to convert.
     * @param from from unit.
     * @param to to unit.
     * @return converted value.
     */
    String convert(@NotNull BigDecimal value, String from, String to);

    /**
     * Get info about unit converter.
     *
     * @return formatted info.
     */
    String getInfo();

    default Pair<BigDecimal, String> calculate(BigDecimal value, BigDecimal fromMultiplier, BigDecimal toMultiplier, RoundingMode roundingMode) {
        BigDecimal result;
        String help;

        if (fromMultiplier.compareTo(toMultiplier) < 0) {
            BigDecimal divisor = toMultiplier.divide(fromMultiplier, roundingMode);
            int resultScale = divisor.toPlainString().length() - 1;
            result = value.setScale(resultScale, roundingMode).divide(divisor, roundingMode);
            help = "/ " + divisor.stripTrailingZeros().toPlainString();
        } else if (fromMultiplier.compareTo(toMultiplier) > 0) {
            BigDecimal multiplier = fromMultiplier.divide(toMultiplier, roundingMode);
            result = value.multiply(multiplier);
            help = "* " + multiplier.stripTrailingZeros().toPlainString();
        } else {
            result = value;
            help = "* 1";
        }

        return Pair.of(result, help);
    }

    default String bigDecimalToString(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

}
