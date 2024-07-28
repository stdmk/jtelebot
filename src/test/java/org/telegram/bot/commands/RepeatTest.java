package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.LastCommand;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.services.LastCommandService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.bot.utils.ObjectCopier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepeatTest {

    private final CommandProperties repeatedCommandProperies = new CommandProperties()
            .setAccessLevel(AccessLevel.TRUSTED.getValue())
            .setClassName(Echo.class.getName())
            .setCommandName("bot");
    private final LastCommand lastCommand = new LastCommand().setCommandProperties(repeatedCommandProperies);

    @Mock
    private ApplicationContext context;
    @Mock
    private ObjectCopier objectCopier;
    @Mock
    private UserService userService;
    @Mock
    private UserStatsService userStatsService;
    @Mock
    private LastCommandService lastCommandService;

    @InjectMocks
    private Repeat repeat;

    @ParameterizedTest
    @ValueSource(strings = {"", "repeat", "repeat 123", ".", ". 123"})
    void parseTest(String command) {
        BotRequest request = TestUtils.getRequestFromGroup(command);

        List<BotResponse> botResponses = repeat.parse(request);
        assertTrue(botResponses.isEmpty());

        verify(userStatsService, never()).incrementUserStatsCommands(any(Chat.class), any(User.class));
        verify(context, never()).getBean(anyString());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "repeat", "repeat 123", ". 123"})
    void analyzeTest(String command) {
        BotRequest request = TestUtils.getRequestFromGroup(command);

        List<BotResponse> botResponses = repeat.analyze(request);
        assertTrue(botResponses.isEmpty());

        verify(userStatsService, never()).incrementUserStatsCommands(any(Chat.class), any(User.class));
        verify(context, never()).getBean(anyString());
    }

    @Test
    void analyzeWithoutLastCommandTest() {
        BotRequest request = TestUtils.getRequestFromGroup(".");

        List<BotResponse> botResponses = repeat.analyze(request);
        assertTrue(botResponses.isEmpty());

        verify(userStatsService, never()).incrementUserStatsCommands(any(Chat.class), any(User.class));
        verify(context, never()).getBean(anyString());
    }

    @Test
    void analyzeWithoutAccessToCommandTest() {
        BotRequest request = TestUtils.getRequestFromGroup(".");
        Message message = request.getMessage();

        when(lastCommandService.get(message.getChat())).thenReturn(lastCommand);
        when(userService.getCurrentAccessLevel(message.getUser().getUserId(), message.getChatId())).thenReturn(AccessLevel.NEWCOMER);
        when(userService.isUserHaveAccessForCommand(AccessLevel.NEWCOMER.getValue(), repeatedCommandProperies.getAccessLevel()))
                .thenReturn(false);

        List<BotResponse> botResponses = repeat.analyze(request);
        assertTrue(botResponses.isEmpty());

        verify(userStatsService, never()).incrementUserStatsCommands(any(Chat.class), any(User.class));
        verify(context, never()).getBean(anyString());
    }

    @Test
    void analyzeWithFailedToCopyRequestTest() {
        BotRequest request = TestUtils.getRequestFromGroup(".");
        Message message = request.getMessage();

        when(lastCommandService.get(message.getChat())).thenReturn(lastCommand);
        when(userService.getCurrentAccessLevel(message.getUser().getUserId(), message.getChatId())).thenReturn(AccessLevel.NEWCOMER);
        when(userService.isUserHaveAccessForCommand(AccessLevel.NEWCOMER.getValue(), repeatedCommandProperies.getAccessLevel()))
                .thenReturn(true);

        List<BotResponse> botResponses = repeat.analyze(request);
        assertTrue(botResponses.isEmpty());

        verify(userStatsService, never()).incrementUserStatsCommands(any(Chat.class), any(User.class));
        verify(context, never()).getBean(anyString());
    }

    @Test
    void analyzeTest() {
        BotRequest request = TestUtils.getRequestFromGroup(".");
        Message message = request.getMessage();

        when(lastCommandService.get(message.getChat())).thenReturn(lastCommand);
        when(userService.getCurrentAccessLevel(message.getUser().getUserId(), message.getChatId())).thenReturn(AccessLevel.NEWCOMER);
        when(userService.isUserHaveAccessForCommand(AccessLevel.NEWCOMER.getValue(), repeatedCommandProperies.getAccessLevel()))
                .thenReturn(true);
        when(objectCopier.copyObject(request, BotRequest.class)).thenReturn(request);
        Command command = mock(Command.class);
        when(context.getBean(repeatedCommandProperies.getClassName())).thenReturn(command);
        BotResponse botResponse2 = mock(BotResponse.class);
        when(command.parse(request)).thenReturn(List.of(botResponse2));

        BotResponse botResponse = repeat.analyze(request).get(0);
        assertEquals(botResponse2, botResponse);

        verify(userStatsService).incrementUserStatsCommands(message.getChat(), message.getUser());
    }

}