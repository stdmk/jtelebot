package org.telegram.bot.domain.model.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.enums.FormattingStyle;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TextResponse implements BotResponse {

    Map<FormattingStyle, ResponseSettings> formattingStyleResponseSettingsMap = Arrays.stream(FormattingStyle.values())
            .map(formattingStyle -> new ResponseSettings().setFormattingStyle(formattingStyle))
            .collect(Collectors.toMap(ResponseSettings::getFormattingStyle, responseSettings1 -> responseSettings1));

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
        this.responseSettings = formattingStyleResponseSettingsMap.get(formattingStyle);
        return this;
    }
}
