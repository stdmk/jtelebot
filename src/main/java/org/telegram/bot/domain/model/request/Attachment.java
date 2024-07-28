package org.telegram.bot.domain.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class Attachment {
    private String mimeType;
    private String fileUniqueId;
    private String fileId;
    private String name;
    private Long size;
    private Integer duration;
}
