package org.telegram.bot.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtils {
    /**
     * Cuts a command from text.
     *
     * @param text - text to be processed.
     * @return text without command.
     */
    public static String cutCommandInText(String text) {
        String cuttedText = getPotentialCommandInText(text);
        if (cuttedText != null) {
            if (text.equals(cuttedText)) {
                return null;
            }
            return text.replace(cuttedText, "").substring(1);
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
        Pattern pattern = Pattern.compile("^\\w+(\\W|$)", Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }

        return null;
    }
}
