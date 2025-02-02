package org.telegram.bot.utils;

import lombok.experimental.UtilityClass;
import org.telegram.bot.domain.entities.User;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class TextUtils {

    public static final Integer TELEGRAM_MESSAGE_TEXT_MAX_LENGTH = 4096;
    public static final String BORDER = "-----------------------------\n";

    private static final Pattern COMMAND_PATTERN = Pattern.compile("^[a-zA-Zа-яА-Я0-9Ёё]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern WORD_PATTERN = Pattern.compile("\\W$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("/[\\w,\\s-]+\\.[A-Za-z]+$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

    public String cutCommandInText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        if (text.charAt(0) == '/') {
            text = text.substring(1);
        }
        String cuttedText = getPotentialCommandInText(text);
        if (cuttedText != null) {
            if (text.toLowerCase(Locale.ROOT).equals(cuttedText)) {
                return null;
            }
            int i = text.indexOf("@");
            if (i > 0 && text.endsWith("bot")) {
                text = text.substring(0, i);
            }
            text = text.substring(cuttedText.length());
            if (text.isEmpty()) {
                return null;
            }
            if (text.startsWith("_")) {
                return text;
            }

            return text.substring(1);
        }

        return null;
    }

    /**
     * Gets a potential command from text.
     *
     * @param text - text to be processed.
     * @return potential command without rest text.
     */
    public static String getPotentialCommandInText(String text) {
        if (text.charAt(0) == '/') {
            text = text.substring(1);
        }
        Matcher matcher = COMMAND_PATTERN.matcher(text);
        if (matcher.find()) {
            String buf = matcher.group(0).trim();
            matcher = WORD_PATTERN.matcher(buf);
            if (matcher.find()) {
                return buf.substring(0, buf.length() - 1).toLowerCase(Locale.ROOT);
            }
            return buf.toLowerCase(Locale.ROOT);
        }

        return null;
    }

    public static String cutMarkdownSymbolsInText(String text) {
        return text.replaceAll("[*_`\\[\\]()]", "").replace("<.*?>","");
    }

    public static String reduceSpaces(String text) {
        while (text.contains("  ")) {
            text = text.replaceAll(" +", " ");
        }
        while (text.contains("\n\n")) {
            text = text.replace("\n\n", "\n");
        }

        return text.trim();
    }

    public static String cutHtmlTags(String text) {
        text = text.replaceAll("<.*?>","");
        return text.replace("<", "");
    }

    public static boolean hasLineBreaks(String text) {
        return text.contains("\n") || text.contains("\r");
    }

    public static String removeDuplicateLineBreaks(String text) {
        while (text.contains("\n\n")) {
            text = text.replace("\n\n", "\n");
        }

        return text;
    }

    public static String withCapital(String text) {
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    public static String removeCapital(String text) {
        return text.substring(0, 1).toLowerCase(Locale.ROOT) + text.substring(1);
    }

    public static boolean startsWithElementInList(String text, List<String> symbolsList) {
        return symbolsList.stream().anyMatch(text::startsWith);
    }

    public static boolean isNotTextLengthIncludedInLimit(String text) {
        return text.length() > TELEGRAM_MESSAGE_TEXT_MAX_LENGTH;
    }

    public static List<String> splitTextByTelegramMaxLength(String text) {
        List<String> result = new ArrayList<>();

        while (text.length() > TELEGRAM_MESSAGE_TEXT_MAX_LENGTH) {
            result.add(text.substring(0, TELEGRAM_MESSAGE_TEXT_MAX_LENGTH));
            text = text.substring(TELEGRAM_MESSAGE_TEXT_MAX_LENGTH + 1);
        }

        result.add(text);

        return result;
    }

    public static String cutIfLongerThan(String text, int limit) {
        if (text.length() > limit) {
            return text.substring(0, limit-3) + "...";
        }

        return text;
    }

    public static String deleteWordsInText(String wordStartsWith, String text) {
        int i = text.indexOf(wordStartsWith);
        while (i >= 0) {
            String word = text.substring(i);
            int endOfWord = word.indexOf(" ");
            if (endOfWord < 0) {
                endOfWord = word.length();
            }
            word = word.substring(0, endOfWord);
            text = text.replace(word, "");
            i = text.indexOf(wordStartsWith);
        }

        return text;
    }

    public static String getLinkToUser(User user, boolean htmlMode) {
        return getLinkToUser(user.getUserId(), htmlMode, user.getUsername());
    }

    public static String getLinkToUser(org.telegram.telegrambots.meta.api.objects.User user, boolean htmlMode) {
        return getLinkToUser(user.getId(), htmlMode, user.getUserName());
    }

    public static String getLinkToUser(User user, boolean htmlMode, String caption) {
        return getLinkToUser(user.getUserId(), htmlMode, caption);
    }

    public static String getLinkToUser(Long userId, boolean htmlMode, String caption) {
        String link = "tg://user?id=" + userId;

        if (htmlMode) {
            return buildHtmlLink(link, caption);
        }

        return buildMarkDownLink(link, caption);
    }

    public static String buildHtmlLink(String link, Object caption) {
        return "<a href=\"" + link + "\">" + caption + "</a>";
    }

    public static String buildMarkDownLink(String link, String caption) {
        return "[" + caption + "](" + link + ")";
    }

    public static String wrapTextToSpoiler(String text) {
        return "<tg-spoiler>" + text + "</tg-spoiler>";
    }

    public static String formatLongValue(long value) {
        final long E = 1000000000000000000L;
        final long P = 1000000000000000L;
        final long T = 1000000000000L;
        final long G = 1000000000L;
        final long M = 1000000L;
        final long K = 1000L;

        if (value > E) {
            return value / E + "E";
        } else if (value > P) {
            return value / P + "P";
        } else if (value > T) {
            return value / T + "T";
        } else if (value > G) {
            return value / G + "G";
        } else if (value > M) {
            return value / M + "M";
        } else if (value > K) {
            return value / K + "K";
        } else {
            return String.valueOf(value);
        }
    }

    public static String formatFileSize(long size) {
        return formatFileSize((double) size);
    }

    public static String formatFileSize(double size) {
        float value = (float) size;
        String unit;
        float result;

        final float G = 1073741824;
        final float M = 1048576;
        final float K = 1024;

        if (size > G) {
            unit = "Gb";
            result = value / G;
        } else if (size > M) {
            unit = "Mb";
            result = value / M;
        } else if (size > K) {
            unit = "Kb";
            result = value / M;
        } else {
            unit = "b";
            result = value;
        }

        return String.format(Locale.ROOT, "%.2f", result) + " " + unit;
    }

    public static boolean startsWithNumber(String text) {
        try {
            Integer.parseInt(text.substring(0, 1));
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    public static Float parseFloat(String text) {
        return Float.parseFloat(text.replace(",", "."));
    }

    public static Double parseDouble(String text) {
        return Double.parseDouble(text.replace(",", "."));
    }

    public static boolean isThatUrl(String text) {
        try {
            new URL(text);
        } catch (MalformedURLException e) {
            return false;
        }

        return true;
    }

    @Nullable
    public static String getFileNameFromUrl(String url) {
        Matcher matcher = FILE_NAME_PATTERN.matcher(url);

        if (matcher.find()) {
            return url.substring(matcher.start() + 1, matcher.end());
        }

        return null;
    }

    public static boolean isThatPositiveInteger(String text) {
        if (text == null) {
            return false;
        }

        return INTEGER_PATTERN.matcher(text).matches();
    }

    public static boolean containsStartWith(Set<String> stringSet, String text) {
        return stringSet.stream().anyMatch(text::startsWith);
    }

    @Nullable
    public static String getStartsWith(Set<String> stringSet, String text) {
        return stringSet.stream().filter(text::startsWith).findFirst().orElse(null);
    }

    public URL findFirstUrlInText(String text) throws MalformedURLException {
        String stringUrl;

        int i = text.indexOf("http");
        if (i < 0) {
            stringUrl = "http://" + text;
        } else {
            text = text.substring(i);
            int spaceIndex = text.indexOf(" ");
            if (spaceIndex < 0) {
                stringUrl = text;
            } else {
                stringUrl = text.substring(0, spaceIndex);
            }
        }

        return new URL(stringUrl);
    }

    public static boolean isNotEmpty(String text) {
        return !isEmpty(text);
    }

    public static boolean isEmpty(String text) {
        return text == null || text.isEmpty();
    }

}
