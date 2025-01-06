package org.telegram.bot.commands;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.LastMessage;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhereTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;
    @Mock
    private UserService userService;
    @Mock
    private UserStatsService userStatsService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private Clock clock;

    @InjectMocks
    private Where where;

    @Test
    void parseFromPrivateChatTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromPrivate("where");

        when(speechService.getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> where.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithoutArgumentTest() {
        final String expectedResponseText = "${command.where.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup("where");
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());

        BotResponse botResponse = where.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
        assertEquals(message.getMessageId(), textResponse.getReplyToMessageId());
    }

    @Test
    void parseWithUnknownUserAsArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("where username");
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());

        List<BotResponse> botResponses = where.parse(request);

        assertTrue(botResponses.isEmpty());
        verify(bot, never()).sendTyping(message.getChatId());
    }

    @Disabled
    @Test
    void parseTest() {
        final String expectedResponseText = "${command.where.lasttime} <b><a href=\"tg://user?id=1\">username</a></b> ${command.where.saw} 03.02.1999 04:05:06 (GMT+03:00)\n" +
                "${command.where.silent} 331 ${utils.date.d}. 19 ${utils.date.h}. 54 ${utils.date.m}. 54 ${utils.date.s}.  (10 ${utils.date.months}. 29 ${utils.date.d}. )";
        final Integer expectedMessageId = 123;
        final String username = "username";
        BotRequest request = TestUtils.getRequestFromGroup("where " + username);
        Message message = request.getMessage();
        LastMessage lastMessage = new LastMessage();
        lastMessage.setMessageId(expectedMessageId);
        lastMessage.setDate(LocalDateTime.of(1999, 2, 3, 4, 5, 6));
        UserStats userStats = new UserStats();
        userStats.setLastMessage(lastMessage);

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(userService.get(username)).thenReturn(message.getUser());
        when(userStatsService.get(message.getChat(), message.getUser())).thenReturn(userStats);
        LocalDateTime currentDateTime = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
        when(clock.instant()).thenReturn(currentDateTime.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        BotResponse botResponse = where.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        assertEquals(expectedMessageId, textResponse.getReplyToMessageId());

        verify(bot).sendTyping(message.getChatId());
    }

}