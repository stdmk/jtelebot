package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class MeTest {

    @Mock
    private SpeechService speechService;

    @InjectMocks
    private Me me;

    @Test
    void parsePrivateMessageTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromPrivate("/me test");

        Mockito.when(speechService.getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> me.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithoutArgumentText() {
        BotRequest request = TestUtils.getRequestFromGroup("/me");

        List<BotResponse> botResponses = me.parse(request);
        assertEquals(1, botResponses.size());

        TestUtils.checkDefaultDeleteResponseParams(botResponses.get(0));
    }

    @Test
    void parseTest() {
        final String expectedResponseText = "* <a href=\"tg://user?id=1\">username</a> test";
        BotRequest request = TestUtils.getRequestFromGroup("/me test");

        List<BotResponse> botResponses = me.parse(request);
        assertEquals(2, botResponses.size());

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponses.get(0));
        assertEquals(expectedResponseText, textResponse.getText());

        TestUtils.checkDefaultDeleteResponseParams(botResponses.get(1));
    }

}