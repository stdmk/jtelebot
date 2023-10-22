package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.commands.Cmd;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

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
        Update update = TestUtils.getUpdateFromGroup("cmd");
        assertThrows(BotException.class, () -> cmd.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWrongCommandTest() {
        Update update = TestUtils.getUpdateFromGroup("cmd test");
        SendMessage sendMessage = cmd.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        checkDefaultSendMessageParams(sendMessage);
    }

    @Test
    void parseTest() {
        Update update = TestUtils.getUpdateFromGroup("cmd help");
        SendMessage sendMessage = cmd.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        checkDefaultSendMessageParams(sendMessage);
    }

}