package org.telegram.bot.utils;

import lombok.experimental.UtilityClass;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageKind;

import java.time.LocalDateTime;

@UtilityClass
public class TelegramUtils {

    private static final Integer MESSAGE_EXPIRATION_TIME_SECONDS = 60;

    public static boolean isThatAnOldEditedMessage(Message message) {
        return MessageKind.EDIT.equals(message.getMessageKind())
                && message.getEditDateTime().plusSeconds(MESSAGE_EXPIRATION_TIME_SECONDS).isBefore(LocalDateTime.now());
    }
}
