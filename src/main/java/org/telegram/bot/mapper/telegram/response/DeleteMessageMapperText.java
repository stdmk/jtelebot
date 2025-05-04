package org.telegram.bot.mapper.telegram.response;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.DeleteResponse;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;

@Component
public class DeleteMessageMapperText implements TelegramTextApiMethodMapper {

    @Override
    public Class<? extends BotResponse> getMappingClass() {
        return DeleteResponse.class;
    }

    @Override
    public PartialBotApiMethod<?> map(BotResponse botResponse) {
        DeleteResponse deleteResponse = (DeleteResponse) botResponse;
        return new DeleteMessage(deleteResponse.getChatId().toString(), deleteResponse.getMessageId());
    }
}
