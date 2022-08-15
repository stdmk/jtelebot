package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
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

import static org.telegram.bot.utils.DateUtils.deltaDatesToString;
import static org.telegram.bot.utils.DateUtils.formatDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimeDelta implements CommandParent<SendMessage> {

    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());

        if (textMessage == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

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
                    try {
                        firstDateTime = LocalDateTime.parse(textMessage.substring(matcher.start(), matcher.end()), dateFormatter);
                    } catch (Exception e) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                } else {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                if (matcher.find()) {
                    secondDateTime = LocalDateTime.parse(textMessage.substring(matcher.start(), matcher.end()), dateFormatter);
                } else {
                    secondDateTime = dateTimeNow;
                }
            } else {
                pattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})");
                DateTimeFormatter timeFormatter = DateUtils.timeFormatter;
                Matcher matcher = pattern.matcher(textMessage);

                if (matcher.find()) {
                    try {
                        firstDateTime = LocalTime.parse(textMessage.substring(matcher.start(), matcher.end()), timeFormatter)
                                .atDate(LocalDate.now());
                    } catch (Exception e) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                } else {
                    pattern = Pattern.compile("(\\d{2}):(\\d{2})");
                    matcher = pattern.matcher(textMessage);

                    if (matcher.find()) {
                        try {
                            firstDateTime = LocalTime.parse(textMessage.substring(matcher.start(), matcher.end()) + ":00", timeFormatter)
                                    .atDate(LocalDate.now());
                        } catch (Exception e) {
                            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                        }
                    } else {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                }

                secondDateTime = dateTimeNow;
            }
        } else {
            pattern = Pattern.compile("(\\d{2})\\.(\\d{2})\\.(\\d{4})");
            dateFormatter = DateUtils.dateFormatter;
            Matcher matcher = pattern.matcher(textMessage);

            if (matcher.find()) {
                try {
                    firstDateTime = LocalDate.parse(textMessage.substring(matcher.start(), matcher.end()), dateFormatter).atStartOfDay();
                } catch (Exception e) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }
            } else {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            if (matcher.find()) {
                secondDateTime = LocalDate.parse(textMessage.substring(matcher.start(), matcher.end()), dateFormatter).atStartOfDay();
            } else {
                secondDateTime = dateTimeNow;
            }
        }

        log.debug("Request to compare {} and {}", formatDateTime(firstDateTime), formatDateTime(secondDateTime));

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText("До " + formatDateTime(firstDateTime) + ":*\n" + deltaDatesToString(firstDateTime, secondDateTime) + "*");

        return sendMessage;
    }
}