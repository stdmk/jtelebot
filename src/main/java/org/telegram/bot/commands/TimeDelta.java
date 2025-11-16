package org.telegram.bot.commands;

import lombok.Getter;
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
import org.telegram.bot.exception.datetime.DateTimeParseException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserCityService;

import javax.annotation.PostConstruct;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.telegram.bot.utils.DateUtils.deltaDatesToString;
import static org.telegram.bot.utils.DateUtils.formatDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimeDelta implements Command {

    private static final Pattern DMY_HMS_DATE_TIME_PATTERN = Pattern.compile ("(\\d{2})\\.(\\d{2})\\.(\\d{4}) (\\d{2}):(\\d{2}):(\\d{2})");
    private static final Pattern DMY_HM_DATE_TIME_PATTERN = Pattern.compile ("(\\d{2})\\.(\\d{2})\\.(\\d{4}) (\\d{2}):(\\d{2})");
    private static final Pattern DM_HMS_DATE_TIME_PATTERN = Pattern.compile ("(\\d{2})\\.(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2})");
    private static final Pattern DM_HM_DATE_TIME_PATTERN = Pattern.compile ("(\\d{2})\\.(\\d{2}) (\\d{2}):(\\d{2})");
    private static final Pattern DMY_DATE_PATTERN = Pattern.compile ("(\\d{2})\\.(\\d{2})\\.(\\d{4})");
    private static final Pattern DM_DATE_PATTERN = Pattern.compile ("(\\d{2})\\.(\\d{2})");
    private static final Pattern HMS_TIME_PATTERN = Pattern.compile ("(\\d{2}):(\\d{2}):(\\d{2})");
    private static final Pattern HM_TIME_PATTERN = Pattern.compile ("(\\d{2}):(\\d{2})");

    private static final DateTimeFormatter DMY_HMS_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final DateTimeFormatter DMY_HM_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DMY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter HMS_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter HM_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final Pattern DAYS_PATTERN = Pattern.compile("-?\\d+");

    private final Map<Pattern, Function<String, LocalDateTime>> dateTimePatterns = new LinkedHashMap<>();

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final UserCityService userCityService;
    private final SpeechService speechService;
    private final Clock clock;

    @PostConstruct
    private void postConstruct() {
        dateTimePatterns.put(DMY_HMS_DATE_TIME_PATTERN, raw -> parseDateTime(raw, DMY_HMS_DATE_TIME_FORMATTER));
        dateTimePatterns.put(DMY_HM_DATE_TIME_PATTERN, raw -> parseDateTime(raw, DMY_HM_DATE_TIME_FORMATTER));
        dateTimePatterns.put(DM_HMS_DATE_TIME_PATTERN, raw -> parseDateTimeWithoutYear(raw, DMY_HMS_DATE_TIME_FORMATTER));
        dateTimePatterns.put(DM_HM_DATE_TIME_PATTERN, raw -> parseDateTimeWithoutYear(raw, DMY_HM_DATE_TIME_FORMATTER));
        dateTimePatterns.put(DMY_DATE_PATTERN, this::parseDate);
        dateTimePatterns.put(DM_DATE_PATTERN, this::parseDateWithoutYear);
        dateTimePatterns.put(HMS_TIME_PATTERN, raw -> parseTime(raw, HMS_TIME_FORMATTER));
        dateTimePatterns.put(HM_TIME_PATTERN, raw -> parseTime(raw, HM_TIME_FORMATTER));
    }

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());

        String commandArgument = commandWaitingService.getText(message);

        String responseText;
        LocalDateTime firstDateTime;
        LocalDateTime secondDateTime;
        if (commandArgument == null) {
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.timedelta.commandwaitingstart}";
        } else {
            ZoneId zoneIdOfUser = userCityService.getZoneIdOfUserOrDefault(message);
            DateTimeData dateTimeData;
            try {
                dateTimeData = getDateTimeDataFromText(commandArgument, zoneIdOfUser);
            } catch (DateTimeParseException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            firstDateTime = dateTimeData.getFirstDateTime();
            secondDateTime = dateTimeData.getSecondDateTime();
            if (firstDateTime == null) {
                firstDateTime = ZonedDateTime.now(clock).withZoneSameInstant(zoneIdOfUser).toLocalDateTime();
                secondDateTime = firstDateTime.plusDays(dateTimeData.getDays());
            } else {
                if (secondDateTime == null) {
                    if (dateTimeData.getDays() != null) {
                        secondDateTime = firstDateTime.plusDays(dateTimeData.getDays());
                    } else {
                        secondDateTime = ZonedDateTime.now(clock).withZoneSameInstant(zoneIdOfUser).toLocalDateTime();
                    }
                }
            }

            responseText = getTimeDelta(firstDateTime, secondDateTime);
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }

    private DateTimeData getDateTimeDataFromText(String text, ZoneId zoneId) {
        String data = text.trim();
        DateTimeData dateTimeData = new DateTimeData();

        for (Map.Entry<Pattern, Function<String, LocalDateTime>> entry : dateTimePatterns.entrySet()) {
            if (dateTimeData.isFilled()) {
                break;
            }

            Pattern pattern = entry.getKey();
            Function<String, LocalDateTime> dateTimeGetter = entry.getValue();

            Matcher matcher = pattern.matcher(data);

            List<String> rawDateTimes = new ArrayList<>(2);
            int i = 0;
            while (matcher.find() && i < 2) {
                rawDateTimes.add(data.substring(matcher.start(), matcher.end()));
                i = i + 1;
            }

            if (!rawDateTimes.isEmpty()) {
                for (String rawDateTime : rawDateTimes) {
                    dateTimeData.addDateTime(dateTimeGetter.apply(rawDateTime).atZone(zoneId).withZoneSameInstant(zoneId).toLocalDateTime());
                    data = data.replaceFirst(rawDateTime, "");
                }
            }
        }

        if (!dateTimeData.isFilled()) {
            Matcher matcher = DAYS_PATTERN.matcher(data);
            if (matcher.find()) {
                dateTimeData.setDays(Integer.parseInt(data.substring(matcher.start(), matcher.end())));
            }
        }

        if (dateTimeData.getFirstDateTime() == null && dateTimeData.getSecondDateTime() == null && dateTimeData.getDays() == null) {
            throw new DateTimeParseException();
        }

        return dateTimeData;
    }

    private String getTimeDelta(LocalDateTime firstDateTime, LocalDateTime secondDateTime) {
        if (firstDateTime.isAfter(secondDateTime)) {
            return buildResponseText(secondDateTime, firstDateTime);
        }

        return buildResponseText(firstDateTime, secondDateTime);
    }

    private String buildResponseText(LocalDateTime firstDateTime, LocalDateTime secondDateTime) {
        return "${command.timedelta.from} " + formatDateTime(firstDateTime) + " ${command.timedelta.to} " + formatDateTime(secondDateTime) +
                ":*\n" + deltaDatesToString(firstDateTime, secondDateTime) + "*";
    }

    private LocalDateTime parseDateTime(String raw, DateTimeFormatter dateTimeFormatter) throws DateTimeParseException {
        try {
            return LocalDateTime.parse(raw, dateTimeFormatter);
        } catch (Exception e) {
            throw new DateTimeParseException();
        }
    }

    private LocalDateTime parseDateTimeWithoutYear(String raw, DateTimeFormatter dateTimeFormatter) {
        try {
            int year = Year.now(clock).getValue();
            Matcher matcher = DM_DATE_PATTERN.matcher(raw);
            if (!matcher.find()) {
                throw new DateTimeParseException();
            }

            String rawWithCurrentYear = raw.substring(matcher.start(), matcher.end()) + "." + year + raw.substring(matcher.end());

            return LocalDateTime.parse(rawWithCurrentYear, dateTimeFormatter);
        } catch (Exception e) {
            throw new DateTimeParseException();
        }
    }

    private LocalDateTime parseDate(String raw) throws DateTimeParseException {
        try {
            return LocalDate.parse(raw, DMY_DATE_FORMATTER).atStartOfDay();
        } catch (Exception e) {
            throw new DateTimeParseException();
        }
    }

    private LocalDateTime parseDateWithoutYear(String raw) {
        try {
            int year = Year.now(clock).getValue();
            Matcher matcher = DM_DATE_PATTERN.matcher(raw);
            if (!matcher.find()) {
                throw new DateTimeParseException();
            }

            String dateWithCurrentYear = raw.substring(matcher.start(), matcher.end()) + "." + year + raw.substring(matcher.end());

            return LocalDate.parse(dateWithCurrentYear, TimeDelta.DMY_DATE_FORMATTER).atStartOfDay();
        } catch (Exception e) {
            throw new DateTimeParseException();
        }
    }

    private LocalDateTime parseTime(String raw, DateTimeFormatter dateFormatter) throws DateTimeParseException {
        try {
            return LocalTime.parse(raw, dateFormatter).atDate(LocalDate.now(clock));
        } catch (Exception e) {
            throw new DateTimeParseException();
        }
    }

    @Getter
    private static class DateTimeData {
        private LocalDateTime firstDateTime = null;
        private LocalDateTime secondDateTime = null;
        private Integer days;

        public void addDateTime(LocalDateTime localDateTime) {
            if (this.firstDateTime == null) {
                this.firstDateTime = localDateTime;
            } else if (this.secondDateTime == null) {
                this.secondDateTime = localDateTime;
            }
        }

        public void setDays(int days) {
            this.days = days;
        }

        public boolean isFilled() {
            return this.firstDateTime != null && this.secondDateTime != null;
        }

    }

}