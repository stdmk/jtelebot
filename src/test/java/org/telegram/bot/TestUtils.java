package org.telegram.bot;

import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

public class TestUtils {

    public static final String BOT_USERNAME = "jtelebot";
    public static final Long DEFAULT_CHAT_ID = 1L;
    public static final String DEFAULT_MESSAGE_TEXT = "test";

    public static Update getUpdate() {
        return getUpdate(DEFAULT_CHAT_ID, DEFAULT_MESSAGE_TEXT);
    }

    public static Update getUpdate(String textMessage) {
        return getUpdate(DEFAULT_CHAT_ID, textMessage);
    }

    public static Update getUpdate(Long chatId, String textMessage) {
        Chat chat = new Chat();
        chat.setId(chatId);

        User user = new User();
        user.setId(1L);

        Message message = new Message();
        message.setChat(chat);
        message.setFrom(user);
        message.setText(textMessage);

        Update update = new Update();
        update.setMessage(message);

        return update;
    }
}
