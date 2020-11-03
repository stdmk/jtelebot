package org.telegram.bot.utils;

public class ExceptionUtils {

    public static String getInitialExceptionCauseText(Exception e) {
        while (e.getCause() != null) {
            e = (Exception) e.getCause();
        }

        return e.getMessage();
    }
}
