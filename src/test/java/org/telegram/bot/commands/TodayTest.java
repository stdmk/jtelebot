package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.MessageStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.MessageStatsService;
import org.telegram.bot.services.SpeechService;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodayTest {

    @Mock
    private MessageStatsService messageStatsService;
    @Mock
    private SpeechService speechService;
    @Mock
    private Bot bot;

    @InjectMocks
    private Today today;

    @Test
    void parseWithArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("today test");
        List<BotResponse> botResponses = today.parse(request);
        assertTrue(botResponses.isEmpty());
        verify(bot, never()).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parsePrivateMessageTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromPrivate("today");

        when(speechService.getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> today.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithoutTopMessageTest() {
        final String expectedResponseText = "${command.today.boringday}";
        BotRequest request = TestUtils.getRequestFromGroup("today");
        Message message = request.getMessage();
        Chat chat = message.getChat();

        when(messageStatsService.getByRepliesCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of());
        when(messageStatsService.getByReactionsCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of());

        BotResponse botResponse = today.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(chat.getChatId());
    }

    @Test
    void parseOnlyRepliesTest() {
        final String expectedResponseText = """
                <b>${command.today.caption}:</b>
                <u>${command.today.byreplies}:</u>
                1) <a href="https://t.me/c/1272607487/1">message1Text</a> (5)

                """;
        BotRequest request = TestUtils.getRequestFromGroup("today");
        Message message = request.getMessage();
        Chat chat = message.getChat();
        chat.setChatId(-1001272607487L);
        MessageStats messageStatsReplies = new MessageStats()
                .setId(1L)
                .setReplies(5)
                .setReactions(0)
                .setMessage(new org.telegram.bot.domain.entities.Message().setMessageId(1).setText("message1Text"));

        when(messageStatsService.getByRepliesCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of(messageStatsReplies));
        when(messageStatsService.getByReactionsCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of());

        BotResponse botResponse = today.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(chat.getChatId());
    }

    @Test
    void parseOnlyReactionTest() {
        final String expectedResponseText = """
                <b>${command.today.caption}:</b>
                
                <u>${command.today.byreactions}:</u>
                1) <a href="https://t.me/c/1272607487/2">message2Text</a> (3)
                """;
        BotRequest request = TestUtils.getRequestFromGroup("today");
        Message message = request.getMessage();
        Chat chat = message.getChat();
        chat.setChatId(-1001272607487L);
        MessageStats messageStatsReactions = new MessageStats()
                .setId(2L)
                .setReplies(0)
                .setReactions(3)
                .setMessage(new org.telegram.bot.domain.entities.Message().setMessageId(2).setText("message2Text"));

        when(messageStatsService.getByRepliesCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of());
        when(messageStatsService.getByReactionsCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of(messageStatsReactions));

        BotResponse botResponse = today.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(chat.getChatId());
    }

    @Test
    void parseTest() {
        final String expectedResponseText = """
                <b>${command.today.caption}:</b>
                <u>${command.today.byreplies}:</u>
                1) <a href="https://t.me/c/1272607487/1">message1Text</a> (5)

                <u>${command.today.byreactions}:</u>
                1) <a href="https://t.me/c/1272607487/2">message2Text</a> (3)
                """;
        BotRequest request = TestUtils.getRequestFromGroup("today");
        Message message = request.getMessage();
        Chat chat = message.getChat();
        chat.setChatId(-1001272607487L);
        MessageStats messageStatsReplies = new MessageStats()
                .setId(1L)
                .setReplies(5)
                .setReactions(0)
                .setMessage(new org.telegram.bot.domain.entities.Message().setMessageId(1).setText("message1Text"));
        MessageStats messageStatsReactions = new MessageStats()
                .setId(2L)
                .setReplies(0)
                .setReactions(3)
                .setMessage(new org.telegram.bot.domain.entities.Message().setMessageId(2).setText("message2Text"));

        when(messageStatsService.getByRepliesCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of(messageStatsReplies));
        when(messageStatsService.getByReactionsCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of(messageStatsReactions));

        BotResponse botResponse = today.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(chat.getChatId());
    }

    @Test
    void analyzePrivateMessageTest() {
        BotRequest request = TestUtils.getRequestFromPrivate("test");

        List<BotResponse> botResponseList = today.analyze(request);

        verify(messageStatsService, never()).incrementReplies(anyInt());
        verify(messageStatsService, never()).incrementReactions(anyInt(), anyInt());

        assertTrue(botResponseList.isEmpty());
    }

    @Test
    void analyzeMessageWithoutRepliesOrReactionsTest() {
        BotRequest request = TestUtils.getRequestFromGroup("test");

        List<BotResponse> botResponseList = today.analyze(request);

        verify(messageStatsService, never()).incrementReplies(anyInt());
        verify(messageStatsService, never()).incrementReactions(anyInt(), anyInt());

        assertTrue(botResponseList.isEmpty());
    }

    @Test
    void analyzeMessageHasReplyToTest() {
        BotRequest request = TestUtils.getRequestWithRepliedMessage("test");

        List<BotResponse> botResponseList = today.analyze(request);

        verify(messageStatsService).incrementReplies(request.getMessage().getReplyToMessage().getMessageId());
        verify(messageStatsService, never()).incrementReactions(anyInt(), anyInt());

        assertTrue(botResponseList.isEmpty());
    }

    @Test
    void analyzeMessageHasReactionsTest() {
        final int reactionsCount = 3;
        BotRequest request = TestUtils.getRequestFromGroup("test");
        Message message = request.getMessage();
        message.setMessageContentType(MessageContentType.REACTION);
        message.setReactionsCount(reactionsCount);

        List<BotResponse> botResponseList = today.analyze(request);

        verify(messageStatsService, never()).incrementReplies(anyInt());
        verify(messageStatsService).incrementReactions(message.getMessageId(), reactionsCount - 1);

        assertTrue(botResponseList.isEmpty());
    }

}