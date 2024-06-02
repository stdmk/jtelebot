package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Error;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ErrorService;
import org.telegram.bot.services.SpeechService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.telegram.bot.TestUtils.*;

@ExtendWith(MockitoExtension.class)
class ErrorsTest {

    @Mock
    private Bot bot;
    @Mock
    private ErrorService errorService;
    @Mock
    private SpeechService speechService;
    @Mock
    private BotStats botStats;

    @InjectMocks
    private Errors errors;

    @Test
    void getErrorListTest() {
        BotRequest request = getRequestFromGroup("errors");
        List<Error> errorList = List.of(new Error().setId(1L).setDateTime(LocalDateTime.now()).setComment("comment"));

        when(errorService.getAll()).thenReturn(errorList);

        BotResponse response = errors.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());
        checkDefaultTextResponseParams(response);
    }

    @Test
    void clearErrorListTest() {
        final String expectedResponseMessage = "saved";
        BotRequest request = getRequestFromGroup("errors_clear");

        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseMessage);

        BotResponse response = errors.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(errorService).clear();
        TextResponse textResponse = checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseMessage, textResponse.getText());
    }

    @Test
    void getErrorDataWithWrongIdTest() {
        final String expectedExceptionMessage = "wrong id";
        BotRequest request = getRequestFromGroup("errors_a");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedExceptionMessage);

        BotException botException = assertThrows(BotException.class, () -> errors.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        assertEquals(expectedExceptionMessage, botException.getMessage());
    }

    @Test
    void getErrorDataOfNotExistenceErrorEntityTest() {
        final long errorId = 1;
        final String expectedExceptionMessage = "not existence Error";
        BotRequest request = getRequestFromGroup("errors_" + errorId);

        when(errorService.get(errorId)).thenReturn(null);
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedExceptionMessage);

        BotException botException = assertThrows(BotException.class, () -> errors.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        assertEquals(expectedExceptionMessage, botException.getMessage());
    }

    @Test
    void getErrorDataWithUnexpectedArgument() {
        BotRequest request = getRequestFromGroup("errors abv");
        List<BotResponse> botResponses = errors.parse(request);
        verify(bot).sendTyping(request.getMessage().getChatId());
        assertTrue(botResponses.isEmpty());
    }

    @Test
    void getErrorDataTest() {
        final long errorId = 1;
        BotRequest request = getRequestFromGroup("errors_" + errorId);
        Error error = new Error()
                .setId(errorId)
                .setDateTime(LocalDateTime.now())
                .setComment("comment")
                .setRequest("request")
                .setResponse("response")
                .setStacktrace("stacktrace");

        when(errorService.get(errorId)).thenReturn(error);

        BotResponse response = errors.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());
        checkDefaultFileResponseParams(response);
    }
}