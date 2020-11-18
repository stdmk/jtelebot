package org.telegram.bot.utils;

import lombok.Getter;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtils {

    private static final String dateTimeFormatString = "dd.MM.yyyy HH:mm:ss";
    private static final String timeFormatString = "HH:mm:ss";

    public static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat(dateTimeFormatString);
    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormatString);
    public static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timeFormatString);

    public static String formatDate(Date date) {
        return dateTimeFormat.format(date);
    }

    public static String formatDate(LocalDateTime date) {
        return date.format(dateTimeFormatter);
    }

    public static String formatTime(Integer seconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.of("UTC")).format(timeFormatter);
    }

    public static String deltaDatesToString(LocalDateTime date1, LocalDateTime date2) {
        ZoneId zoneId = ZoneId.systemDefault();
        ZoneOffset zoneOffSet = zoneId.getRules().getOffset(LocalDateTime.now());

        long start = date1.toInstant(zoneOffSet).toEpochMilli();
        long end = date2.toInstant(zoneOffSet).toEpochMilli();

        return deltaDatesToString(start - end);
    }

    public static String deltaDatesToString(long milliseconds) {
        StringBuilder responseText = new StringBuilder();

        long days =  milliseconds / 86400000;
        if (days > 0) {
            responseText.append(days).append(" д. ");
        }

        long hours = milliseconds % 86400000 / 3600000;
        if (hours > 0) {
            responseText.append(hours).append(" ч. ");
        }

        long minutes = milliseconds % 86400000 % 3600000 / 60000;
        if (minutes > 0) {
            responseText.append(minutes).append(" м. ");
        }

        long seconds = milliseconds % 86400000 % 3600000 % 60000 / 1000;
        if (seconds > 0) {
            responseText.append(seconds).append(" с. ");
        }

        return responseText.toString();
    }

    @Getter
    public enum TimeZones {
        MINUS_ONE("GMT-01:00"),
        MINUS_TWO("GMT-02:00"),
        MINUS_THREE("GMT-03:00"),
        MINUS_FOUR("GMT-04:00"),
        MINUS_FIVE("GMT-05:00"),
        MINUS_SIX("GMT-06:00"),
        MINUS_SEVEN("GMT-07:00"),
        MINUS_EIGHT("GMT-08:00"),
        MINUS_NINE("GMT-09:00"),
        MINUS_TEN("GMT-10:00"),
        MINUS_ELEVEN("GMT-11:00"),
        MINUS_TWELVE("GMT-12:00"),
        GREENWICH("GMT+00:00"),
        PLUS_ONE("GMT+01:00"),
        PLUS_TWO("GMT+02:00"),
        PLUS_THREE("GMT+03:00"),
        PLUS_FOUR("GMT+04:00"),
        PLUS_FIVE("GMT+05:00"),
        PLUS_SIX("GMT+06:00"),
        PLUS_SEVEN("GMT+07:00"),
        PLUS_EIGHT("GMT+08:00"),
        PLUS_NINE("GMT+09:00"),
        PLUS_TEN("GMT+10:00"),
        PLUS_ELEVEN("GMT+11:00"),
        PLUS_TWELVE("GMT+12:00");

        private final String zone;

        TimeZones(String zone) {
            this.zone = zone;
        }
    }
}
