package org.telegram.bot;

import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

public class TestUtils {
    public static Update getUpdate() {
        return getUpdate("test");
    }

    public static Update getUpdate(String textMessage) {
        Chat chat = new Chat();
        chat.setId(-1L);

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
