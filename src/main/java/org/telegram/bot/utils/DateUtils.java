package org.telegram.bot.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class DateUtils {

    public static String deltaDatesToString(LocalDateTime date1, LocalDateTime date2) {
        ZoneId zoneId = ZoneId.systemDefault();
        ZoneOffset zoneOffset = ZoneOffset.of(zoneId.toString());

        long start = date1.toInstant(zoneOffset).toEpochMilli();
        long end = date2.toInstant(zoneOffset).toEpochMilli();

        return deltaDatesToString(start - end);
    }

    public static String deltaDatesToString(long milliseconds) {
        StringBuilder responseText = new StringBuilder();

        long days =  milliseconds / 86400000;
        if (days > 0) {
            responseText.append(" д. ");
        }

        long hours = milliseconds % 86400000 / 3600000;
        if (hours > 0) {
            responseText.append(" ч. ");
        }

        long minutes = milliseconds % 86400000 % 3600000 / 60000;
        if (minutes > 0) {
            responseText.append(" м. ");
        }

        long seconds = milliseconds % 86400000 % 3600000 % 60000 / 1000;
        if (seconds > 0) {
            responseText.append(" с. ");
        }

        return responseText.toString();
    }
}
