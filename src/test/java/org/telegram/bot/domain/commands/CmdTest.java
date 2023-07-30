package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.telegram.bot.TestUtils.checkDefaultSendMessageParams;

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
        assertThrows(BotException.class, () -> cmd.parse(TestUtils.getUpdateFromGroup("cmd")));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWrongCommandTest() {
        SendMessage sendMessage = cmd.parse(TestUtils.getUpdateFromGroup("cmd test"));
        checkDefaultSendMessageParams(sendMessage);
    }

    @Test
    void parseTest() {
        SendMessage sendMessage = cmd.parse(TestUtils.getUpdateFromGroup("cmd help"));
        checkDefaultSendMessageParams(sendMessage);
    }

}