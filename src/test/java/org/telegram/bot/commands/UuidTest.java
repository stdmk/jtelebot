package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UuidTest {

    private final Uuid uuidCommand = new Uuid();

    @Test
    void parseWithoutArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("uuid");

        BotResponse botResponse = uuidCommand.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        String responseUuid = textResponse.getText().replace("`", "");
        assertDoesNotThrow(() -> UUID.fromString(responseUuid));
    }

    @Test
    void parseWithInvalidUuidAsArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("uuid tratatam");

        BotResponse botResponse = uuidCommand.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals("*${command.uuid.invalid}*", textResponse.getText());
    }

    @Test
    void parseWithValidUuidAsArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("uuid " + UUID.randomUUID());

        BotResponse botResponse = uuidCommand.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals("*${command.uuid.valid}*", textResponse.getText());
    }

}