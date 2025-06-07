package org.telegram.bot.timers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.DelayCommand;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.DelayCommandService;
import org.telegram.bot.services.UserStatsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DelayCommandTimerTest {

    @Mock
    private Bot bot;
    @Mock
    private BotStats botStats;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private DelayCommandService delayCommandService;
    @Mock
    private UserStatsService userStatsService;

    @InjectMocks
    private DelayCommandTimer delayCommandTimer;

    @Test
    void executeWithoutDelayedCommandsTest() {
        when(delayCommandService.getAllBeforeDateTime(any(LocalDateTime.class))).thenReturn(List.of());

        delayCommandTimer.execute();

        verify(delayCommandService).getAllBeforeDateTime(any(LocalDateTime.class));
        verify(bot, never()).processRequest(any(BotRequest.class));
        verify(delayCommandService, never()).remove(any(DelayCommand.class));
    }

    @Test
    void executeWithDelayedCommandsTest() throws JsonProcessingException {
        final String corruptedJson = "corruptedJson";
        final String validJson = "validJson";
        final String jsonProcessingExceptionErrorMessage = "error";

        BotRequest request = TestUtils.getRequestFromGroup();
        LocalDateTime now = LocalDateTime.now();

        DelayCommand notTimeDelayedCommand = new DelayCommand().setDateTime(now.plusDays(1));
        DelayCommand corruptedJsonDelayCommand = new DelayCommand().setDateTime(now).setRequestJson(corruptedJson);
        DelayCommand validJsonDelayCommand = new DelayCommand().setDateTime(now).setRequestJson(validJson);
        List<DelayCommand> delayCommandList = List.of(notTimeDelayedCommand, corruptedJsonDelayCommand, validJsonDelayCommand);
        when(delayCommandService.getAllBeforeDateTime(any(LocalDateTime.class))).thenReturn(delayCommandList);

        JsonProcessingException jsonProcessingException = mock(JsonProcessingException.class);
        when(jsonProcessingException.getMessage()).thenReturn(jsonProcessingExceptionErrorMessage);
        when(objectMapper.readValue(corruptedJson, BotRequest.class)).thenThrow(jsonProcessingException);
        when(objectMapper.readValue(validJson, BotRequest.class)).thenReturn(request);

        delayCommandTimer.execute();

        Message message = request.getMessage();
        verify(userStatsService, times(1)).incrementUserStatsCommands(message.getChat(), message.getUser());
        verify(botStats, times(1)).incrementErrors(corruptedJson, jsonProcessingException, "Failed to deserialize BotRequest: " + jsonProcessingExceptionErrorMessage);
        verify(delayCommandService).remove(corruptedJsonDelayCommand);
        verify(delayCommandService).remove(validJsonDelayCommand);
        verify(delayCommandService, never()).remove(notTimeDelayedCommand);
    }

}