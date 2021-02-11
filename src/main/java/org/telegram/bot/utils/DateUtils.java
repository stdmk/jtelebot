package org.telegram.bot.utils;

import lombok.Getter;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtils {

    private static final String DATE_TIME_FORMAT_STRING = "dd.MM.yyyy HH:mm:ss";
    private static final String TIME_FORMAT_STRING = "HH:mm:ss";
    private static final String DATE_FORMAT_STRING = "dd.MM.yyyy";
    private static final String DATE_TIME_TV_FORMAT_STRING = "dd.MM.yyyy HH:mm";
    private static final String TIME_TV_FORMAT_STRING = "HH:mm";

    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat(DATE_TIME_FORMAT_STRING);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_STRING);
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT_STRING);
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_STRING);
    private static final DateTimeFormatter dateTimeTvFormatter = DateTimeFormatter.ofPattern(DATE_TIME_TV_FORMAT_STRING);
    private static final DateTimeFormatter timeTvFormatter = DateTimeFormatter.ofPattern(TIME_TV_FORMAT_STRING);


    public static String formatDate(Date date) {
        return dateTimeFormat.format(date);
    }

    public static String formatDate(LocalDateTime dateTime) {
        return dateFormatter.format(dateTime);
    }

    public static String formatDate(ZonedDateTime date) {
        return date.format(dateTimeFormatter);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(dateTimeFormatter);
    }

    public static String formatDateTime(ZonedDateTime dateTime) {
        return dateTime.format(dateTimeFormatter);
    }

    public static String formatTvDateTime(LocalDateTime dateTime, ZoneId zoneId) {
        return dateTimeTvFormatter.format(ZonedDateTime.of(dateTime, zoneId));
    }

    public static String formatTime(Integer seconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.of("UTC")).format(timeFormatter);
    }

    public static String formatTvTime(LocalDateTime dateTime, ZoneId zoneId) {
        return timeTvFormatter.format(ZonedDateTime.of(dateTime, zoneId));
    }

    public static String formatTime(ZonedDateTime dateTime) {
        return dateTime.format(timeFormatter);
    }

    public static String deltaDatesToString(LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd) {
        return deltaDatesToString(getDuration(dateTimeStart, dateTimeEnd));
    }

    public static Long getDuration(LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd) {
        ZoneId zoneId = ZoneId.systemDefault();
        ZoneOffset zoneOffSet = zoneId.getRules().getOffset(LocalDateTime.now());

        return getDuration(dateTimeStart, dateTimeEnd, zoneOffSet);
    }

    public static Long getDuration(LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd, ZoneId zoneId) {
        ZoneOffset zoneOffSet = zoneId.getRules().getOffset(LocalDateTime.now());

        return getDuration(dateTimeStart, dateTimeEnd, zoneOffSet);
    }

    public static Long getDuration(LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd, ZoneOffset zoneOffset) {
        return dateTimeEnd.toInstant(zoneOffset).toEpochMilli() - dateTimeStart.toInstant(zoneOffset).toEpochMilli();
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

    public static LocalDateTime atStartOfDay(LocalDateTime localDateTime) {
        return localDateTime.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toLocalDateTime();
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
