package org.telegram.bot.utils;

import lombok.experimental.UtilityClass;
import org.telegram.bot.domain.entities.UserEmail;

@UtilityClass
public class EmailUtils {

    public static boolean isShippingEnabled(UserEmail userEmail) {
        return userEmail != null
                && Boolean.TRUE.equals(userEmail.getShippingEnabled())
                && Boolean.TRUE.equals(userEmail.getVerified())
                && userEmail.getEmail() != null && !userEmail.getEmail().isBlank();
    }

}
