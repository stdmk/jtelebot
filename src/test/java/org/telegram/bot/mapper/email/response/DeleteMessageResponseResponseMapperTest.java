package org.telegram.bot.mapper.email.response;

import org.junit.jupiter.api.Test;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.DeleteResponse;
import org.telegram.bot.domain.model.response.EmailResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeleteMessageResponseResponseMapperTest {

    private final DeleteMessageResponseResponseMapper deleteMessageResponseResponseMapper = new DeleteMessageResponseResponseMapper();

    @Test
    void getMappingClassTest() {
        Class<DeleteResponse> expected = DeleteResponse.class;
        Class<? extends BotResponse> actual = deleteMessageResponseResponseMapper.getMappingClass();
        assertEquals(expected, actual);
    }

    @Test
    void mapTest() {
        final int messageId = 1;
        DeleteResponse response = new DeleteResponse(new Chat(), messageId);

        EmailResponse emailResponse = deleteMessageResponseResponseMapper.map(response);

        assertNotNull(emailResponse);
        assertEquals("${mapper.email.delete.caption}: " + messageId, emailResponse.getText());
    }

}