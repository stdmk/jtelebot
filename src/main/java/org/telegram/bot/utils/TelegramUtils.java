package org.telegram.bot.utils;

import lombok.experimental.UtilityClass;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageKind;

import java.time.LocalDateTime;

@UtilityClass
public class TelegramUtils {

    private static final Integer MESSAGE_EXPIRATION_TIME_SECONDS = 60;

    public static boolean isUnsupportedMessage(Message message) {
        return message == null
                || (MessageKind.EDIT.equals(message.getMessageKind())
                        && message.getEditDateTime().plusSeconds(MESSAGE_EXPIRATION_TIME_SECONDS).isBefore(LocalDateTime.now()));
    }

    public static boolean isPrivateChat(Chat chat) {
        return chat.getChatId() > 0;
    }

}
