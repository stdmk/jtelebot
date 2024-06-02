package org.telegram.bot.domain.model.response;

import lombok.Data;
import lombok.experimental.Accessors;
import org.telegram.bot.enums.FormattingStyle;

@Data
@Accessors(chain = true)
public class ResponseSettings {
    private FormattingStyle formattingStyle;
    private boolean webPagePreview;
    private boolean notification;
}
