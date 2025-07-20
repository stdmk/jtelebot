package org.telegram.bot;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.telegram.bot.commands.MessageAnalyzer;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.mapper.telegram.response.ResponseMapper;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.executors.MethodExecutor;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;

import java.util.List;

import static org.mockito.Mockito.*;

class ParserTest {

    private final MessageAnalyzer messageAnalyzer1 = mock(MessageAnalyzer.class);
    private final MessageAnalyzer messageAnalyzer2 = mock(MessageAnalyzer.class);
    private final MessageAnalyzer messageAnalyzer3 = mock(MessageAnalyzer.class);
    private final MessageAnalyzer messageAnalyzer4 = mock(MessageAnalyzer.class);
    private final MessageAnalyzer messageAnalyzer5 = mock(MessageAnalyzer.class);
    private final MessageAnalyzer messageAnalyzer6 = mock(MessageAnalyzer.class);
    private final CommandPropertiesService commandPropertiesService = mock(CommandPropertiesService.class);
    private final UserService userService = mock(UserService.class);
    private final BotStats botStats = mock(BotStats.class);
    private final ResponseMapper responseMapper = mock(ResponseMapper.class);
    private final MethodExecutor methodExecutor = mock(MethodExecutor.class);

    private final Parser parser = new Parser(
            responseMapper,
            List.of(methodExecutor),
            botStats,
            List.of(messageAnalyzer1, messageAnalyzer2, messageAnalyzer3, messageAnalyzer4, messageAnalyzer5, messageAnalyzer6),
            commandPropertiesService,
            userService);

    @Captor
    private ArgumentCaptor<List<BotResponse>> botResponseCaptor;

    @Test
    void analyzeMessageAsyncTest() {
        AccessLevel userAccessLevel = AccessLevel.FAMILIAR;
        BotRequest request = TestUtils.getRequestFromGroup();

        CommandProperties commandProperties2 = new CommandProperties().setAccessLevel(10);
        CommandProperties commandProperties3 = new CommandProperties().setAccessLevel(1);

        when(commandPropertiesService.getCommand(messageAnalyzer1.getClass()))
                .thenReturn(null)
                .thenReturn(commandProperties2)
                .thenReturn(commandProperties3)
                .thenReturn(commandProperties3)
                .thenReturn(commandProperties3)
                .thenReturn(commandProperties3)
                .thenReturn(commandProperties3);
        when(messageAnalyzer3.analyze(request)).thenReturn(null);
        when(messageAnalyzer4.analyze(request)).thenReturn(List.of());
        List<BotResponse> botResponses = List.of(new TextResponse());
        when(messageAnalyzer5.analyze(request)).thenReturn(botResponses);
        RuntimeException runtimeException = mock(RuntimeException.class);
        when(messageAnalyzer6.analyze(request)).thenThrow(runtimeException);
        when(userService.isUserHaveAccessForCommand(1, 10)).thenReturn(false);
        when(userService.isUserHaveAccessForCommand(1, 1)).thenReturn(true);
        PartialBotApiMethod telegramMethod = mock(PartialBotApiMethod.class);
        when(telegramMethod.getMethod()).thenReturn("method");
        when(responseMapper.toTelegramMethod(botResponses)).thenReturn(List.of(telegramMethod));
        when(methodExecutor.getMethod()).thenReturn("method");

        parser.analyzeMessageAsync(request, userAccessLevel);

        verify(methodExecutor).executeMethod(telegramMethod, request);
        verify(botStats).incrementErrors(request, runtimeException, "Unexpected general error: ");
    }

}