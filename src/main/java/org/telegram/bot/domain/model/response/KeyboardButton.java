package org.telegram.bot.domain.model.response;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class KeyboardButton {
    private String name;
    private String callback;
}
