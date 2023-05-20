package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.telegram.bot.TestUtils.checkDefaultSendMessageParams;
import static org.telegram.bot.TestUtils.getUpdate;

@ExtendWith(MockitoExtension.class)
class CmdTest {
    @Mock
    private SpeechService speechService;

    @InjectMocks
    private Cmd cmd;

    @Test
    void parseEmptyParamsTest() {
        assertThrows(BotException.class, () -> cmd.parse(getUpdate("cmd")));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWrongCommandTest() {
        SendMessage sendMessage = cmd.parse(getUpdate("cmd test"));
        checkDefaultSendMessageParams(sendMessage);
    }

    @Test
    void parseTest() {
        SendMessage sendMessage = cmd.parse(getUpdate("cmd help"));
        checkDefaultSendMessageParams(sendMessage);
    }

}