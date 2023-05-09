package org.telegram.bot;

import org.telegram.telegrambots.meta.api.objects.*;

public class TestUtils {

    public static final String BOT_USERNAME = "jtelebot";
    public static final Long DEFAULT_CHAT_ID = 1L;
    public static final String DEFAULT_MESSAGE_TEXT = "test";

    public static Update getUpdate() {
        return getUpdate(DEFAULT_CHAT_ID, DEFAULT_MESSAGE_TEXT);
    }

    public static Update getUpdateWithCallback(String callback) {
        Chat chat = new Chat();
        chat.setId(DEFAULT_CHAT_ID);

        User user = new User();
        user.setId(1L);

        Message message = new Message();
        message.setChat(chat);
        message.setFrom(user);

        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setFrom(user);
        callbackQuery.setMessage(message);
        callbackQuery.setData(callback);

        Update update = new Update();
        update.setCallbackQuery(callbackQuery);

        return update;
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
