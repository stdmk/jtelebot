package org.telegram.bot.utils;

import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@UtilityClass
public class TelegramUtils {

    private static final Integer MESSAGE_EXPIRATION_TIME_SECONDS = 60;

    public static Message getMessage(Update update) {
        Message message = update.getMessage();
        if (message == null) {
            message = update.getEditedMessage();
            if (message == null) {
                message = update.getCallbackQuery().getMessage();
            }
        }

        return message;
    }

    public static boolean isThatAnOldMessage(Message message) {
        Integer editDate = message.getEditDate();
        if (editDate != null) {
            return editDate - message.getDate() > MESSAGE_EXPIRATION_TIME_SECONDS;
        }

        return false;
    }
}
