package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.commands.convertors.UnitsConverter;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Converter implements Command {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+[.,]*\\d*)\\s?([a-zA-Zа-яА-Я]+)\\.?\\s?([a-zA-Zа-яА-Я]+)$");

    private final List<UnitsConverter> converters;
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

            responseText = converters
                    .stream()
                    .map(converter -> converter.convert(value, fromUnit, toUnit))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n\n"));
        }

        if (!StringUtils.hasText(responseText)) {
            responseText = converters.stream().map(UnitsConverter::getInfo).collect(Collectors.joining("\n"));
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML));
    }

}
