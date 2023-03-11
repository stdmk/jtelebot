package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
import java.util.regex.Pattern;

import static org.telegram.bot.utils.DateUtils.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimeDelta implements CommandParent<SendMessage> {

    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        String responseText;
        if (textMessage == null) {
            commandWaitingService.add(message, this.getClass());
            responseText = "теперь напиши мне что нужно посчитать";
        } else {
            Pattern pattern;
            DateTimeFormatter dateFormatter;
            LocalDateTime dateTimeNow = LocalDateTime.now();
            LocalDateTime firstDateTime;
            LocalDateTime secondDateTime;

            if (textMessage.indexOf(":") > 0) {
                if (textMessage.indexOf(".") > 0) {
                    pattern = Pattern.compile("(\\d{2})\\.(\\d{2})\\.(\\d{4}) (\\d{2}):(\\d{2}):(\\d{2})");
                    dateFormatter = DateUtils.dateTimeFormatter;
                    Matcher matcher = pattern.matcher(textMessage);

                    if (matcher.find()) {
                        firstDateTime = parseLocalDateTimeFromText(textMessage.substring(matcher.start(), matcher.end()), dateFormatter);
                    } else {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }

                    if (matcher.find()) {
                        secondDateTime = parseLocalDateTimeFromText(textMessage.substring(matcher.start(), matcher.end()), dateFormatter);
                    } else {
                        secondDateTime = dateTimeNow;
                    }
                } else {
                    pattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})");
                    DateTimeFormatter timeFormatter = DateUtils.timeFormatter;
                    Matcher matcher = pattern.matcher(textMessage);

                    if (matcher.find()) {
                        firstDateTime = parseLocalTimeFromText(textMessage.substring(matcher.start(), matcher.end()), timeFormatter)
                                .atDate(LocalDate.now());
                    } else {
                        pattern = Pattern.compile("(\\d{2}):(\\d{2})");
                        matcher = pattern.matcher(textMessage);

                        if (matcher.find()) {
                            firstDateTime = parseLocalTimeFromText(textMessage.substring(matcher.start(), matcher.end()) + ":00", timeFormatter)
                                    .atDate(LocalDate.now());
                        } else {
                            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                        }
                    }

                    secondDateTime = dateTimeNow;
                }
                responseText = "От " + formatDateTime(firstDateTime) + " до " + formatDateTime(secondDateTime) +
                        ":*\n" + deltaDatesToString(firstDateTime, secondDateTime) + "*";
            } else {
                pattern = Pattern.compile("(\\d{2})\\.(\\d{2})\\.(\\d{4})");
                dateFormatter = DateUtils.dateFormatter;
                Matcher matcher = pattern.matcher(textMessage);

                String textFirstDate;
                if (matcher.find()) {
                    textFirstDate = textMessage.substring(matcher.start(), matcher.end());
                    firstDateTime = parseLocalDateFromText(textFirstDate, dateFormatter).atStartOfDay();
                } else {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                if (matcher.find()) {
                    secondDateTime = parseLocalDateFromText(textMessage.substring(matcher.start(), matcher.end()), dateFormatter)
                            .atStartOfDay();
                    responseText = "От " + formatDate(firstDateTime) + " до " + formatDate(secondDateTime) +
                            ":*\n" + deltaDatesToString(firstDateTime, secondDateTime) + "*";
                } else {
                    if (textMessage.length() != textFirstDate.length()) {
                        int days;

                        try {
                            days = Integer.parseInt(textMessage.substring(textFirstDate.length() + 1));
                        } catch (NumberFormatException e) {
                            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                        }

                        secondDateTime = firstDateTime.plusDays(days);

                        responseText = formatDate(firstDateTime) + " *" + String.format("%+d", days) + "* = *" + formatDate(secondDateTime) + "*";
                    } else {
                        secondDateTime = dateTimeNow;
                        responseText = "До " + formatDate(firstDateTime) + ":*\n" + deltaDatesToString(firstDateTime, secondDateTime) + "*";
                    }
                }
            }

            log.debug("Request to compare {} and {}", formatDateTime(firstDateTime), formatDateTime(secondDateTime));
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(responseText);

        return sendMessage;
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