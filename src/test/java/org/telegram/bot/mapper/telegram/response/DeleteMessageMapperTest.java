package org.telegram.bot.mapper.telegram.response;

import org.junit.jupiter.api.Test;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.model.response.DeleteResponse;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeleteMessageMapperTest {

    private final DeleteMessageMapperText deleteMessageMapper = new DeleteMessageMapperText();

    @Test
    void mapTest() {
        final Long chatId = 123L;
        final Integer messageId = 12345;
        DeleteResponse deleteResponse = new DeleteResponse(new Chat().setChatId(chatId), messageId);

        DeleteMessage deleteMessage = (DeleteMessage) deleteMessageMapper.map(deleteResponse);

        assertEquals(chatId.toString(), deleteMessage.getChatId());
        assertEquals(messageId, deleteMessage.getMessageId());
    }

}