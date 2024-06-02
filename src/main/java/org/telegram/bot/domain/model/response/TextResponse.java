package org.telegram.bot.domain.model.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.enums.FormattingStyle;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TextResponse implements BotResponse {
    private Long chatId;
    private String text;
    private Integer replyToMessageId;
    private Keyboard keyboard;
    private ResponseSettings responseSettings;

    public TextResponse(Message message) {
        this.chatId = message.getChatId();
        this.replyToMessageId = message.getMessageId();
    }

    @Tolerate
    public TextResponse setResponseSettings(FormattingStyle formattingStyle) {
        this.responseSettings = new ResponseSettings().setFormattingStyle(formattingStyle);
        return this;
    }
}
