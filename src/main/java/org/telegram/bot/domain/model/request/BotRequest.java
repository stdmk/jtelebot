package org.telegram.bot.domain.model.request;

import lombok.Data;
import lombok.experimental.Accessors;
import org.telegram.bot.enums.RequestSource;

@Data
@Accessors(chain = true)
public class BotRequest {
    private Message message;
    private RequestSource source;
}
