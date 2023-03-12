package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CmdTest {
    @Mock
    SpeechService speechService;

    @InjectMocks
    Cmd cmd;

    @Test
    void parseEmptyParamsTest() {
        assertThrows(BotException.class, () -> cmd.parse(TestUtils.getUpdate("cmd")));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWrongCommandTest() {
        SendMessage sendMessage = cmd.parse(TestUtils.getUpdate("cmd test"));
        assertNotNull(sendMessage);
        assertNotNull(sendMessage.getText());
    }

    @Test
    void parseTest() {
        SendMessage sendMessage = cmd.parse(TestUtils.getUpdate("cmd help"));
        assertNotNull(sendMessage);
        assertNotNull(sendMessage.getText());
    }

}