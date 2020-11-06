package org.telegram.bot.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtils {
    private static final String dateTimeFormatString = "dd.MM.yyyy HH:mm:ss";
    public static SimpleDateFormat dateTimeFormat = new SimpleDateFormat(dateTimeFormatString);
    public static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormatString);

    public static String formatDate(Date date) {
        return dateTimeFormat.format(date);
    }

    public static String formatDate(LocalDateTime date) {
        return date.format(dateTimeFormatter);
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
}
