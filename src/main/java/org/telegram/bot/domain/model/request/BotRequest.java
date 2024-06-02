package org.telegram.bot.domain.model.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BotRequest {
    private Message message;
}
