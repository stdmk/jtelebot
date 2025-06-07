package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.timers.FileManagerTimer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShutdownTest {

    @Mock
    private Bot bot;
    @Mock
    private ConfigurableApplicationContext configurableApplicationContext;
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
        verify(configurableApplicationContext, never()).close();
    }

    @Test
    void parseWithSaveStatsExceptionTest() {
        final String expectedResponseText = "";
        BotRequest request = TestUtils.getRequestFromGroup("shutdown");
        doThrow(RuntimeException.class).when(botStats).saveStats();

        BotResponse botResponse = shutdown.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(botStats).saveStats();
        verify(fileManagerTimer).deleteAllFiles();
        verify(configurableApplicationContext).close();
    }

    @Test
    void parseWithDeleteFilesExceptionTest() {
        final String expectedResponseText = "";
        BotRequest request = TestUtils.getRequestFromGroup("shutdown");
        doThrow(RuntimeException.class).when(fileManagerTimer).deleteAllFiles();

        BotResponse botResponse = shutdown.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(botStats).saveStats();
        verify(fileManagerTimer).deleteAllFiles();
        verify(configurableApplicationContext).close();
    }

    @Test
    void parseTest() {
        final String expectedResponseText = "";
        BotRequest request = TestUtils.getRequestFromGroup("shutdown");

        BotResponse botResponse = shutdown.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(botStats).saveStats();
        verify(fileManagerTimer).deleteAllFiles();
        verify(configurableApplicationContext).close();
    }

}