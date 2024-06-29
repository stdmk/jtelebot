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
import org.telegram.bot.services.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        BotRequest request = TestUtils.getRequestFromGroup("getid test");

        when(userService.get(anyString())).thenReturn(null);

        assertThrows(BotException.class, () -> getid.parse(request));

        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void getIdOfUsernameTest() {
        final String responseText = """
                ${command.getid.id} [username](tg://user?id=1): `1`
                ${command.getid.groupid}: `-1`
                ${command.getid.yourid}: `1`""";
        BotRequest request = TestUtils.getRequestFromGroup("getid test");

        when(userService.get(anyString())).thenReturn(TestUtils.getUser());

        BotResponse response = getid.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(responseText, textResponse.getText());
    }

    @Test
    void getIdOfUserFromRepliedMessageTest() {
        final String responseText = """
                ${command.getid.id} [username](tg://user?id=2): `2`
                ${command.getid.groupid}: `-1`
                ${command.getid.yourid}: `1`""";
        BotRequest request = TestUtils.getRequestWithRepliedMessage("getid");

        BotResponse response = getid.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(responseText, textResponse.getText());
    }

    @Test
    void getIdInGroupChatTest() {
        final String responseText = "${command.getid.groupid}: `-1`\n" +
                "${command.getid.yourid}: `1`";
        BotRequest request = TestUtils.getRequestFromGroup("getid");

        BotResponse response = getid.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(responseText, textResponse.getText());
    }

    @Test
    void getIdTest() {
        final String responseText = "${command.getid.yourid}: `1`";
        BotRequest request = TestUtils.getRequestFromPrivate("getid");

        BotResponse response = getid.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(responseText, textResponse.getText());
    }

}