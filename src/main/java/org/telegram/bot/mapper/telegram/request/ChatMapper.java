package org.telegram.bot.mapper.telegram.request;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.Chat;

@Component
public class ChatMapper {

    public Chat toChat(org.telegram.telegrambots.meta.api.objects.chat.Chat chat) {
        Long chatId = chat.getId();

        String chatName;
        if (chatId > 0) {
            chatName = chat.getUserName();
            if (chatName == null) {
                chatName = chat.getFirstName();
            }
        } else {
            chatName = chat.getTitle();
            if (chatName == null) {
                chatName = "";
            }
        }

        return new Chat().setChatId(chatId).setName(chatName);
    }

}
