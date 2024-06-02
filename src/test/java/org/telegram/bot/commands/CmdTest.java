package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.telegram.bot.TestUtils.checkDefaultTextResponseParams;

@ExtendWith(MockitoExtension.class)
class CmdTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;

    @InjectMocks
    private Cmd cmd;

    @Test
    void parseEmptyParamsTest() {
        BotRequest request = TestUtils.getRequestFromGroup("cmd");
        assertThrows(BotException.class, () -> cmd.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWrongCommandTest() {
        BotRequest request = TestUtils.getRequestFromGroup("cmd test");
        BotResponse botResponse = cmd.parse(request).get(0);

        TextResponse textResponse = checkDefaultTextResponseParams(botResponse);
        verify(bot).sendTyping(request.getMessage().getChatId());
        checkDefaultTextResponseParams(textResponse);
    }

    @Test
    void parseTest() {
        BotRequest request = TestUtils.getRequestFromGroup("cmd help");
        BotResponse botResponse = cmd.parse(request).get(0);

        TextResponse textResponse = checkDefaultTextResponseParams(botResponse);
        verify(bot).sendTyping(request.getMessage().getChatId());
        checkDefaultTextResponseParams(textResponse);
    }

}