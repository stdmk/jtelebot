package org.telegram.bot.domain.model.response;

import lombok.Data;
import lombok.experimental.Accessors;
import org.telegram.bot.domain.model.request.Message;

@Data
@Accessors(chain = true)
public class LocationResponse implements BotResponse {
    private Long chatId;
    private Integer replyToMessageId;
    private Double latitude;
    private Double longitude;

    public LocationResponse(Message message) {
        this.chatId = message.getChatId();
        this.replyToMessageId = message.getMessageId();
    }
}
