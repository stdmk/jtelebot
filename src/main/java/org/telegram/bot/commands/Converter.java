package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.commands.convertors.UnitsConverter;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Converter implements Command<SendMessage> {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+[.,]*\\d*)\\s?([a-zA-Zа-яА-Я]+)\\.?\\s?([a-zA-Zа-яА-Я]+)$");

    private final List<UnitsConverter> converters;
    private final Bot bot;
    private final SpeechService speechService;

    @Override
    public List<SendMessage> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = cutCommandInText(message.getText());

        String responseText = null;
        if (textMessage != null) {
            Matcher matcher = PATTERN.matcher(textMessage);
            if (!matcher.find()) {
                log.debug("Wrong input: {}", textMessage);
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

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);

        return returnOneResult(sendMessage);
    }

}
