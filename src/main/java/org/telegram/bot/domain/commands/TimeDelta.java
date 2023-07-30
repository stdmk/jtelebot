package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.DateUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;

import static org.telegram.bot.utils.DateUtils.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimeDelta implements CommandParent<SendMessage> {

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        String responseText;
        if (textMessage == null) {
            commandWaitingService.add(message, this.getClass());
            responseText = "теперь напиши мне что нужно посчитать";
        } else {
            LocalDateTime dateTimeNow = LocalDateTime.now();

            if (textMessage.indexOf(":") > 0) {
                if (textMessage.indexOf(".") > 0) {
                    responseText = getDateByLocalDateTimeFormat(textMessage, dateTimeNow);
                } else {
                    responseText = getDatesByTimeFormat(textMessage, dateTimeNow);
                }
            } else {
                responseText = getDatesByDateFormat(textMessage, dateTimeNow);
            }
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String getDateByLocalDateTimeFormat(String text, LocalDateTime dateTimeNow) {
        LocalDateTime firstDateTime;
        LocalDateTime secondDateTime;

        DateTimeFormatter dateFormatter = DateUtils.dateTimeFormatter;
        Matcher matcher = FULL_DATE_TIME_PATTERN.matcher(text);

        if (matcher.find()) {
            firstDateTime = parseLocalDateTimeFromText(text.substring(matcher.start(), matcher.end()), dateFormatter);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (matcher.find()) {
            secondDateTime = parseLocalDateTimeFromText(text.substring(matcher.start(), matcher.end()), dateFormatter);
        } else {
            secondDateTime = dateTimeNow;
        }

        return buildResponseText(firstDateTime, secondDateTime);
    }

    private String getDatesByTimeFormat(String text, LocalDateTime dateTimeNow) {
        LocalDateTime firstDateTime;

        DateTimeFormatter timeFormatter = DateUtils.timeFormatter;
        Matcher matcher = FULL_TIME_PATTERN.matcher(text);

        if (matcher.find()) {
            firstDateTime = parseLocalTimeFromText(text.substring(matcher.start(), matcher.end()), timeFormatter)
                    .atDate(LocalDate.now());
        } else {
            matcher = SHORT_TIME_PATTERN.matcher(text);

            if (matcher.find()) {
                firstDateTime = parseLocalTimeFromText(text.substring(matcher.start(), matcher.end()) + ":00", timeFormatter)
                        .atDate(LocalDate.now());
            } else {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }

        return buildResponseText(firstDateTime, dateTimeNow);
    }

    private String getDatesByDateFormat(String text, LocalDateTime dateTimeNow) {
        LocalDateTime firstDateTime;
        LocalDateTime secondDateTime;

        DateTimeFormatter dateFormatter = DateUtils.dateFormatter;
        Matcher matcher = FULL_DATE_PATTERN.matcher(text);

        String textFirstDate;
        if (matcher.find()) {
            textFirstDate = text.substring(matcher.start(), matcher.end());
            firstDateTime = parseLocalDateFromText(textFirstDate, dateFormatter).atStartOfDay();
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        String responseText;
        if (matcher.find()) {
            secondDateTime = parseLocalDateFromText(text.substring(matcher.start(), matcher.end()), dateFormatter)
                    .atStartOfDay();
            responseText = buildResponseText(firstDateTime, secondDateTime);
        } else {
            if (text.length() != textFirstDate.length()) {
                int days;

                try {
                    days = Integer.parseInt(text.substring(textFirstDate.length() + 1));
                } catch (NumberFormatException e) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                secondDateTime = firstDateTime.plusDays(days);

                responseText = formatDateTime(firstDateTime) + " *" + String.format("%+d", days) + "* = *" + formatDateTime(secondDateTime) + "*";
            } else {
                secondDateTime = dateTimeNow;
                responseText = "До " + formatDateTime(firstDateTime) + ":*\n" + deltaDatesToString(firstDateTime, secondDateTime) + "*";
            }
        }

        return responseText;
    }

    private String buildResponseText(LocalDateTime firstDateTime, LocalDateTime secondDateTime) {
        if (firstDateTime.isAfter(secondDateTime)) {
            return buildResponseText(secondDateTime, firstDateTime);
        } else {
            return "От " + formatDateTime(firstDateTime) + " до " + formatDateTime(secondDateTime) +
                    ":*\n" + deltaDatesToString(firstDateTime, secondDateTime) + "*";
        }
    }

    private LocalDateTime parseLocalDateTimeFromText(String text, DateTimeFormatter formatter) {
        try {
            return LocalDateTime.parse(text, formatter);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private LocalTime parseLocalTimeFromText(String text, DateTimeFormatter formatter) {
        try {
            return LocalTime.parse(text, formatter);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private LocalDate parseLocalDateFromText(String text, DateTimeFormatter formatter) {
        try {
            return LocalDate.parse(text, formatter);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }
}