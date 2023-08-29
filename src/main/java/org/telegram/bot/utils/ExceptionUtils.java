package org.telegram.bot.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ExceptionUtils {

    public static String getInitialExceptionCauseText(Exception e) {
        while (e.getCause() != null) {
            e = (Exception) e.getCause();
        }

        return e.getMessage();
    }
}
