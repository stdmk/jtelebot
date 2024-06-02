package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.DateUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;

import static org.telegram.bot.utils.DateUtils.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimeDelta implements Command {

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());

        String commandArgument = commandWaitingService.getText(message);

        String responseText;
        if (commandArgument == null) {
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.timedelta.commandwaitingstart}";
        } else {
            LocalDateTime dateTimeNow = LocalDateTime.now();

            if (commandArgument.indexOf(":") > 0) {
                if (commandArgument.indexOf(".") > 0) {
                    responseText = getDateByLocalDateTimeFormat(commandArgument, dateTimeNow);
                } else {
                    responseText = getDatesByTimeFormat(commandArgument, dateTimeNow);
                }
            } else {
                responseText = getDatesByDateFormat(commandArgument, dateTimeNow);
            }
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.MARKDOWN));
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
                responseText = "${command.timedelta.to} " + formatDateTime(firstDateTime) + ":*\n" + deltaDatesToString(firstDateTime, secondDateTime) + "*";
            }
        }

        return responseText;
    }

    private String buildResponseText(LocalDateTime firstDateTime, LocalDateTime secondDateTime) {
        if (firstDateTime.isAfter(secondDateTime)) {
            return buildResponseText(secondDateTime, firstDateTime);
        } else {
            return "${command.timedelta.from} " + formatDateTime(firstDateTime) + " ${command.timedelta.to} " + formatDateTime(secondDateTime) +
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