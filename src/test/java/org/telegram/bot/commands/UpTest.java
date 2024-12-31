package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpTest {

    private final Up up = new Up();

    @Test
    void parseWithArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("up tratatam-tratatam");
        List<BotResponse> botResponses = up.parse(request);
        assertTrue(botResponses.isEmpty());
    }

    @Test
    void parseTest() {
        final String expectedResponseText = ".\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n${command.up.caption}";
        BotRequest request = TestUtils.getRequestFromGroup("up");

        BotResponse botResponse = up.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }
}