package org.telegram.bot.mapper.email.response;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.EmailResponse;
import org.telegram.bot.domain.model.response.FileResponse;

@Component
public class FileResponseResponseMapper implements EmailResponseMapper {

    @Override
    public Class<? extends BotResponse> getMappingClass() {
        return FileResponse.class;
    }

    @Override
    public EmailResponse map(BotResponse botResponse) {
        FileResponse fileResponse = (FileResponse) botResponse;

        return new EmailResponse()
                .setAttachments(fileResponse.getFiles())
                .setText(fileResponse.getText());
    }
}
