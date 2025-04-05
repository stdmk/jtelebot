package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.commands.convertors.MetricUnit;
import org.telegram.bot.commands.convertors.Unit;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Converter implements Command {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+[.,]*\\d*)\\s?([a-zA-Zа-яА-Я]+)\\.?\\s?([a-zA-Zа-яА-Я]+)$");

    private final List<Unit> units;
    private final Bot bot;
    private final SpeechService speechService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        String commandArgument = message.getCommandArgument();

        String responseText = null;
        if (commandArgument != null) {
            Matcher matcher = PATTERN.matcher(commandArgument);
            if (!matcher.find()) {
                log.debug("Wrong input: {}", commandArgument);
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            BigDecimal value = new BigDecimal(matcher.group(1).replace(",", "."));
            String fromUnit = matcher.group(2);
            String toUnit = matcher.group(3);

            responseText = units
                    .stream()
                    .map(unit -> convert(unit, value, fromUnit, toUnit))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n\n"));
        }

        if (!StringUtils.hasText(responseText)) {
            responseText = units.stream().map(this::getInfo).collect(Collectors.joining("\n"));
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML));
    }

    private String convert(Unit unit, BigDecimal value, String from, String to) {
        MetricUnit fromUnit = getByAbbreviate(unit, from);
        MetricUnit toUnit = getByAbbreviate(unit, to);
        if (fromUnit == null || toUnit == null) {
            return null;
        }

        Pair<BigDecimal, String> resultPair = calculate(value, fromUnit.getMultiplier(), toUnit.getMultiplier());

        BigDecimal result = resultPair.getFirst();
        String help = resultPair.getSecond();

        return bigDecimalToString(value) + " " + fromUnit.getName() + " = <b>"
                + bigDecimalToString(result) + " " + toUnit.getName() + "</b>\n( " + help + ")";
    }

    private Pair<BigDecimal, String> calculate(BigDecimal value, BigDecimal fromMultiplier, BigDecimal toMultiplier) {
        BigDecimal result;
        String help;

        if (fromMultiplier.compareTo(toMultiplier) < 0) {
            BigDecimal divisor = toMultiplier.divide(fromMultiplier, RoundingMode.HALF_UP);
            int resultScale = divisor.toPlainString().length() - 1;
            result = value.setScale(resultScale, RoundingMode.HALF_UP).divide(divisor, RoundingMode.HALF_UP);
            help = "/ " + divisor.stripTrailingZeros().toPlainString();
        } else if (fromMultiplier.compareTo(toMultiplier) > 0) {
            BigDecimal multiplier = fromMultiplier.divide(toMultiplier, RoundingMode.HALF_UP);
            result = value.multiply(multiplier);
            help = "* " + multiplier.stripTrailingZeros().toPlainString();
        } else {
            result = value;
            help = "* 1";
        }

        return Pair.of(result, help);
    }

    private String getInfo(Unit unit) {
        String availableUnits = unit.getMetricUnitAbbreviaturesSetMap().entrySet()
                .stream()
                .map(metricUnitSetEntry -> metricUnitSetEntry.getKey().getName() + " — " + String.join(",", metricUnitSetEntry.getValue()))
                .collect(Collectors.joining("\n"));
        return "<b>" + unit.getCaption() + "</b>\n" + availableUnits + "\n";
    }

    private MetricUnit getByAbbreviate(Unit unit, String abbr) {
        for (Map.Entry<MetricUnit, Set<String>> entry : unit.getMetricUnitAbbreviaturesSetMap().entrySet()) {
            if (entry.getValue().contains(abbr)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private String bigDecimalToString(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

}
