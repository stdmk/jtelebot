package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserStatsService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WriteTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;
    @Mock
    private ChatService chatService;
    @Mock
    private UserStatsService userStatsService;

    @InjectMocks
    private Write write;

    @ParameterizedTest
    @ValueSource(strings = {"", "a", "a a", "1 a", "0 a"})
    void parseWrongArgumentTest(String command) {
        final String errorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("write " + command);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(errorText);

        BotException botException = assertThrows((BotException.class), () -> write.parse(request));

        assertEquals(errorText, botException.getMessage());
    }

    @Test
    void parseWithUnknownIdAsArgumentTest() {
        final String errorText = "error";
        final long chatId = -1L;
        BotRequest request = TestUtils.getRequestFromGroup("write " + chatId + " a");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(errorText);

        BotException botException = assertThrows((BotException.class), () -> write.parse(request));

        assertEquals(errorText, botException.getMessage());
    }

    @Test
    void parseWithNotUsersChatAsArgumentTest() {
        final String errorText = "error";
        final long chatId = -1L;
        BotRequest request = TestUtils.getRequestFromGroup("write " + chatId + " a");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(errorText);
        Chat chat = new Chat();
        when(chatService.get(chatId)).thenReturn(chat);
        when(userStatsService.getUsersOfChat(chat)).thenReturn(List.of(new User().setUserId(TestUtils.ANOTHER_USER_ID)));

        BotException botException = assertThrows((BotException.class), () -> write.parse(request));

        assertEquals(errorText, botException.getMessage());
    }

    @Test
    void parseTest() {
        final String expectedText = "<a href=\"tg://user?id=1\">username</a> (Telegram): a";
        final long chatId = -1L;
        BotRequest request = TestUtils.getRequestFromGroup("write " + chatId + " a");

        Chat chat = new Chat();
        when(chatService.get(chatId)).thenReturn(chat);
        when(userStatsService.getUsersOfChat(chat)).thenReturn(List.of(request.getMessage().getUser()));

        List<BotResponse> responses = write.parse(request);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());

        ArgumentCaptor<TextResponse> textResponseCaptor = ArgumentCaptor.forClass(TextResponse.class);
        verify(bot).sendMessage(textResponseCaptor.capture());

        TextResponse textResponse = textResponseCaptor.getValue();
        TestUtils.checkDefaultTextResponseParams(textResponse);
        assertEquals(chatId, textResponse.getChatId());
        assertEquals(FormattingStyle.HTML, textResponse.getResponseSettings().getFormattingStyle());
        assertEquals(expectedText, textResponse.getText());
    }

}