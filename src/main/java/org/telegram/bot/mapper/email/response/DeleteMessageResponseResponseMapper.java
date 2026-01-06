package org.telegram.bot.mapper.email.response;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.DeleteResponse;
import org.telegram.bot.domain.model.response.EmailResponse;

@Component
public class DeleteMessageResponseResponseMapper implements EmailResponseMapper {

    @Override
    public Class<? extends BotResponse> getMappingClass() {
        return DeleteResponse.class;
    }

    @Override
    public EmailResponse map(BotResponse botResponse) {
        DeleteResponse deleteResponse = (DeleteResponse) botResponse;

        return new EmailResponse()
                .setText("${mapper.email.delete.caption}: " + deleteResponse.getMessageId());
    }
}
