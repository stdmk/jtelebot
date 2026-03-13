package org.telegram.bot.domain.model.response;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FileParams {
    private long duration;
}
