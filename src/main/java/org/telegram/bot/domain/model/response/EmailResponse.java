package org.telegram.bot.domain.model.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Set;

@Data
@Accessors(chain = true)
public class EmailResponse {
    private Set<String> emailAddresses;
    private String subject;
    private String text;
    private List<File> attachments;
}
