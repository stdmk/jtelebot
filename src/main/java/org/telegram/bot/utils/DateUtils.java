package org.telegram.bot.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

@UtilityClass
public class DateUtils {

    private static final String DATE_TIME_FORMAT_STRING = "dd.MM.yyyy HH:mm:ss";
    private static final String TIME_FORMAT_STRING = "HH:mm:ss";
    private static final String DATE_FORMAT_STRING = "dd.MM.yyyy";
    private static final String DATE_TIME_WITHOUT_SECONDS_FORMAT_STRING = "dd.MM.yyyy HH:mm";
    private static final String TIME_WITHOUT_SECONDS_FORMAT_STRING = "HH:mm";
    private static final String DATE_TIME_WITHOUT_YEAR_AND_SECONDS_FORMAT_STRING = "dd.MM HH:mm";
    public static final String DATE_WITHOUT_DAY_FORMAT_STRING = "MM.yyyy";

    public static final Pattern FULL_DATE_TIME_PATTERN = Pattern.compile("(\\d{2})\\.(\\d{2})\\.(\\d{4}) (\\d{2}):(\\d{2}):(\\d{2})");
    public static final Pattern FULL_DATE_PATTERN = Pattern.compile("(\\d{2})\\.(\\d{2})\\.(\\d{4})");
    public static final Pattern FULL_TIME_PATTERN = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})");
    public static final Pattern SHORT_TIME_PATTERN = Pattern.compile("(\\d{2}):(\\d{2})");

    public final SimpleDateFormat dateTimeFormat = new SimpleDateFormat(DATE_TIME_FORMAT_STRING);
    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_STRING);
    public static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT_STRING);
    public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_STRING);
    public static final DateTimeFormatter dateTimeTvFormatter = DateTimeFormatter.ofPattern(DATE_TIME_WITHOUT_SECONDS_FORMAT_STRING);
    public static final DateTimeFormatter timeShortFormatter = DateTimeFormatter.ofPattern(TIME_WITHOUT_SECONDS_FORMAT_STRING);
    public static final DateTimeFormatter dateTimeShortFormatter = DateTimeFormatter.ofPattern(DATE_TIME_WITHOUT_YEAR_AND_SECONDS_FORMAT_STRING);
    public static final DateTimeFormatter dateWithoutDayFormatter = DateTimeFormatter.ofPattern(DATE_WITHOUT_DAY_FORMAT_STRING);

    public static String formatDate(Date date) {
        return dateTimeFormat.format(date);
    }

    public static String formatDate(LocalDate date) {
        return dateFormatter.format(date);
    }

    public static String formatDate(LocalDateTime dateTime) {
        return dateFormatter.format(dateTime);
    }

    public static String formatDate(ZonedDateTime date) {
        return date.format(dateTimeFormatter);
    }

    public static String formatDate(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(dateFormatter);
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

    public static String formatTime(LocalTime time) {
        return time.format(timeFormatter);
    }

    public static String formatDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(dateTimeFormatter);
    }

    public static String formatShortDateTime(LocalDateTime dateTime) {
        return dateTime.format(dateTimeShortFormatter);
    }

    public static String formatShortDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(dateTimeShortFormatter);
    }

    public static String formatDateWithoutDay(LocalDate date) {
        return date.format(dateWithoutDayFormatter);
    }

    public static String formatShortTime(LocalTime time) {
        return timeShortFormatter.format(time);
    }

    public static String formatTvTime(LocalDateTime dateTime, ZoneId zoneId) {
        return timeShortFormatter.format(ZonedDateTime.of(dateTime, zoneId));
    }

    public static String formatTime(ZonedDateTime dateTime) {
        return dateTime.format(timeFormatter);
    }

    public static String deltaDatesToString(LocalDateTime firstDateTime, LocalDateTime secondDateTime) {
        Period period;
        Duration duration;
        if (firstDateTime.isAfter(secondDateTime)) {
            period = Period.between(secondDateTime.toLocalDate(), firstDateTime.toLocalDate());
            duration = Duration.between(secondDateTime, firstDateTime);
        } else {
            period = Period.between(firstDateTime.toLocalDate(), secondDateTime.toLocalDate());
            duration = Duration.between(firstDateTime, secondDateTime);
        }

        StringBuilder buf = new StringBuilder();
        buf.append(durationToString(duration));

        if (!period.isZero()) {
            buf.append(" (");
            int years = period.getYears();
            if (years != 0) {
                String postfix;
                String yearsCount = String.valueOf(years);

                if (Arrays.asList("11", "12", "13", "14", "15", "16", "17", "18", "19").contains(yearsCount)) {
                    postfix = " ${utils.date.years}. ";
                } else if (yearsCount.endsWith("1") || yearsCount.endsWith("2") || yearsCount.endsWith("3") || yearsCount.endsWith("4")) {
                    postfix = " ${utils.date.year}. ";
                } else {
                    postfix = " ${utils.date.years}. ";
                }

                buf.append(years).append(postfix);
            }

            int months = period.getMonths();
            if (months != 0) {
                buf.append(months).append(" ${utils.date.months}. ");
            }

            int days = period.getDays();
            if (days != 0) {
                buf.append(days).append(" ${utils.date.d}. ");
            }

            buf.append(")");
        }

        return buf.toString();
    }

    public static String durationToString(LocalDateTime firstDateTime, LocalDateTime secondDateTime) {
        Duration duration;
        if (firstDateTime.isAfter(secondDateTime)) {
            duration = Duration.between(secondDateTime, firstDateTime);
        } else {
            duration = Duration.between(firstDateTime, secondDateTime);
        }

        return durationToString(duration);
    }

    public static String durationToString(LocalTime firstTime, LocalTime secondTime) {
        Duration duration;
        if (firstTime.isAfter(secondTime)) {
            duration = Duration.between(secondTime, firstTime);
        } else {
            duration = Duration.between(firstTime, secondTime);
        }

        return durationToString(duration);
    }

    public static String durationToString(long milliseconds) {
        return durationToString(Duration.of(milliseconds, ChronoUnit.MILLIS));
    }

    public static String durationToString(Duration duration) {
        StringBuilder buf = new StringBuilder();
        long days = duration.toDaysPart();
        if (days != 0) {
            buf.append(days).append(" ${utils.date.d}. ");
        }

        int hours = duration.toHoursPart();
        if (hours != 0) {
            buf.append(hours).append(" ${utils.date.h}. ");
        }

        long minutes = duration.toMinutesPart();
        if (minutes != 0) {
            buf.append(minutes).append(" ${utils.date.m}. ");
        }

        long seconds = duration.toSecondsPart();
        if (seconds != 0) {
            buf.append(seconds).append(" ${utils.date.s}. ");
        }

        if (buf.isEmpty()) {
            return "0 с.";
        }

        return buf.toString();
    }

    public static LocalDateTime unixTimeToLocalDateTime(Integer time, ZoneId zoneId) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(time), zoneId);
    }

    public static LocalDateTime unixTimeToLocalDateTime(Integer time) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneId.systemDefault());
    }

    public static LocalDateTime unixTimeToLocalDateTimeUtc(long time) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneId.of("UTC"));
    }

    public static String getDayOfWeek(LocalDateTime dateTime, String lang) {
        return dateTime.getDayOfWeek().getDisplayName(TextStyle.SHORT, getLocale(lang)) + ".";
    }

    public static String getDayOfWeek(LocalDate date, String lang) {
        return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, getLocale(lang)) + ".";
    }

    private Locale getLocale(String lang) {
        if (lang != null) {
            return new Locale(lang);
        }

        return Locale.getDefault();
    }

    public static Duration getDuration(LocalTime timeStart, LocalTime timeEnd) {
        return Duration.between(timeStart, timeEnd);
    }

    public static Duration getDuration(LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd) {
        ZoneId zoneId = ZoneId.systemDefault();
        ZoneOffset zoneOffSet = zoneId.getRules().getOffset(LocalDateTime.now());

        return getDuration(dateTimeStart, dateTimeEnd, zoneOffSet);
    }

    public static Duration getDuration(LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd, ZoneId zoneId) {
        ZoneOffset zoneOffSet = zoneId.getRules().getOffset(LocalDateTime.now());

        return getDuration(dateTimeStart, dateTimeEnd, zoneOffSet);
    }

    public static Duration getDuration(LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd, ZoneOffset zoneOffset) {
        return Duration.between(dateTimeStart.atOffset(zoneOffset), dateTimeEnd.atOffset(zoneOffset));
    }

    public static LocalDateTime atStartOfDay(LocalDateTime localDateTime) {
        return localDateTime.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toLocalDateTime();
    }

    @Getter
    @RequiredArgsConstructor
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
    }
}
