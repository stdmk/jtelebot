package org.telegram.bot.domain.model.response;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.enums.FormattingStyle;

@Data
@Accessors(chain = true)
public class EditResponse implements BotResponse {
    private Long chatId;
    private String text;
    private Integer editableMessageId;
    private Keyboard keyboard;
    private ResponseSettings responseSettings;

    public EditResponse(Message message) {
        this.chatId = message.getChatId();
        this.editableMessageId = message.getMessageId();
    }

    @Tolerate
    public EditResponse setResponseSettings(FormattingStyle formattingStyle) {
        this.responseSettings = new ResponseSettings().setFormattingStyle(formattingStyle);
        return this;
    }

}
