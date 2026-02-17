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
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.ShutdownService;
import org.telegram.bot.timers.FileManagerTimer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShutdownTest {

    @Mock
    private Bot bot;
    @Mock
    private ShutdownService shutdownService;
    @Mock
    private BotStats botStats;
    @Mock
    private FileManagerTimer fileManagerTimer;

    @InjectMocks
    private Shutdown shutdown;

    @Test
    void parseWithArgumentsTest() {
        BotRequest request = TestUtils.getRequestFromGroup("shutdown test");
        List<BotResponse> botResponses = shutdown.parse(request);
        assertTrue(botResponses.isEmpty());
        verify(shutdownService, never()).shutdown();
    }

    @Test
    void parseWithSaveStatsExceptionTest() {
        BotRequest request = TestUtils.getRequestFromGroup("shutdown");
        doThrow(RuntimeException.class).when(botStats).saveStats();

        List<BotResponse> botResponses = shutdown.parse(request);
        assertTrue(botResponses.isEmpty());

        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(botStats).saveStats();
        verify(fileManagerTimer).deleteAllFiles();
        verify(shutdownService).shutdown();
    }

    @Test
    void parseWithDeleteFilesExceptionTest() {
        BotRequest request = TestUtils.getRequestFromGroup("shutdown");
        doThrow(RuntimeException.class).when(fileManagerTimer).deleteAllFiles();

        List<BotResponse> botResponses = shutdown.parse(request);
        assertTrue(botResponses.isEmpty());

        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(botStats).saveStats();
        verify(fileManagerTimer).deleteAllFiles();
        verify(shutdownService).shutdown();
    }

    @Test
    void parseTest() {
        BotRequest request = TestUtils.getRequestFromGroup("shutdown");

        List<BotResponse> botResponses = shutdown.parse(request);
        assertTrue(botResponses.isEmpty());

        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(botStats).saveStats();
        verify(fileManagerTimer).deleteAllFiles();
        verify(shutdownService).shutdown();
    }

}