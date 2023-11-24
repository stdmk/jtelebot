package org.telegram.bot.commands;

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
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetidTest {

    @Mock
    private Bot bot;
    @Mock
    private UserService userService;
    @Mock
    private SpeechService speechService;

    @InjectMocks
    private Getid getid;

    @Test
    void getIdOfUnknownUsernameTest() {
        Update update = TestUtils.getUpdateFromGroup("getid test");

        when(userService.get(anyString())).thenReturn(null);

        assertThrows(BotException.class, () -> getid.parse(update));

        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void getIdOfUsernameTest() {
        final String responseText = "${command.getid.id} [username](tg://user?id=1): `1`\n" +
                "${command.getid.groupid}: `-1`\n" +
                "${command.getid.yourid}: `1`";
        Update update = TestUtils.getUpdateFromGroup("getid test");

        when(userService.get(anyString())).thenReturn(TestUtils.getUser());

        SendMessage method = getid.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        SendMessage sendMessage = TestUtils.checkDefaultSendMessageParams(method);

        assertEquals(responseText, sendMessage.getText());
    }

    @Test
    void getIdOfUserFromRepliedMessageTest() {
        final String responseText = "${command.getid.id} [username](tg://user?id=1): `1`\n" +
                "${command.getid.groupid}: `-1`\n" +
                "${command.getid.yourid}: `1`";
        Update update = TestUtils.getUpdateWithRepliedMessage("getid");

        when(userService.get(anyLong())).thenReturn(TestUtils.getUser());

        SendMessage method = getid.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        SendMessage sendMessage = TestUtils.checkDefaultSendMessageParams(method);

        assertEquals(responseText, sendMessage.getText());
    }

    @Test
    void getIdInGroupChatTest() {
        final String responseText = "${command.getid.groupid}: `-1`\n" +
                "${command.getid.yourid}: `1`";
        Update update = TestUtils.getUpdateFromGroup("getid");

        SendMessage method = getid.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        SendMessage sendMessage = TestUtils.checkDefaultSendMessageParams(method);

        assertEquals(responseText, sendMessage.getText());
    }

    @Test
    void getIdTest() {
        final String responseText = "${command.getid.yourid}: `1`";
        Update update = TestUtils.getUpdateFromPrivate("getid");

        SendMessage method = getid.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        SendMessage sendMessage = TestUtils.checkDefaultSendMessageParams(method);

        assertEquals(responseText, sendMessage.getText());
    }

}