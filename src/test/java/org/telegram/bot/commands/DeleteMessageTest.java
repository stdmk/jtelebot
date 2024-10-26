package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteMessageTest {

    private final DeleteMessage deleteMessage = new DeleteMessage();

    @Test
    void parseWithArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("delete test");
        List<BotResponse> botResponses = deleteMessage.parse(request);
        assertTrue(botResponses.isEmpty());
    }

    @Test
    void parseWithoutReplyMessageTest() {
        BotRequest request = TestUtils.getRequestFromGroup("delete");
        List<BotResponse> botResponses = deleteMessage.parse(request);

        assertEquals(1, botResponses.size());
        TestUtils.checkDefaultDeleteResponseParams(botResponses.get(0));
    }

    @Test
    void parseTest() {
        BotRequest request = TestUtils.getRequestWithRepliedMessage("delete");
        List<BotResponse> botResponses = deleteMessage.parse(request);

        assertEquals(2, botResponses.size());
        botResponses.forEach(TestUtils::checkDefaultDeleteResponseParams);
    }

}