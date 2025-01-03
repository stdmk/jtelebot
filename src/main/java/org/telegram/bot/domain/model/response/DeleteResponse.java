package org.telegram.bot.domain.model.response;

import lombok.Data;
import lombok.experimental.Accessors;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.model.request.Message;

@Data
@Accessors(chain = true)
public class DeleteResponse implements BotResponse {

    private Long chatId;
    private Integer messageId;

    public DeleteResponse(Message message) {
        this.chatId = message.getChatId();
        this.messageId = message.getMessageId();
    }

    public DeleteResponse(Chat chat, Integer messageId) {
        this.chatId = chat.getChatId();
        this.messageId = messageId;
    }

}
