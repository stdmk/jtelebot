package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.Emoji;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UuidTest {

    private final Uuid uuidCommand = new Uuid();

    @Test
    void parseWithoutArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("uuid");

        BotResponse botResponse = uuidCommand.parse(request).getFirst();

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        String responseUuid = textResponse.getText().replace("<code>", "")
                .replace("</code>", "");
        assertDoesNotThrow(() -> UUID.fromString(responseUuid));
    }

    @Test
    void parseWithInvalidUuidAsArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("uuid tratatam");

        BotResponse botResponse = uuidCommand.parse(request).getFirst();

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(Emoji.DELETE.getSymbol() + " UUID <b>${command.uuid.invalid}</b>", textResponse.getText());
    }

    @Test
    void parseWithValidUuidAsArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("uuid " + UUID.randomUUID());

        BotResponse botResponse = uuidCommand.parse(request).getFirst();

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(Emoji.CHECK_MARK.getSymbol() + " UUID <b>${command.uuid.valid}</b>", textResponse.getText());
    }

}