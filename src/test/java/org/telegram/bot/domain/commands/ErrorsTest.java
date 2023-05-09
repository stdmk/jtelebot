package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Error;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ErrorService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorsTest {

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
        Update update = TestUtils.getUpdate("errors");
        List<Error> errorList = List.of(new Error().setId(1L).setDateTime(LocalDateTime.now()).setComment("comment"));

        when(errorService.getAll()).thenReturn(errorList);

        PartialBotApiMethod<?> method = errors.parse(update);
        assertTrue(method instanceof SendMessage);

        SendMessage sendMessage = (SendMessage) method;
        assertNotNull(sendMessage.getText());
    }

    @Test
    void clearErrorListTest() {
        final String expectedResponseMessage = "saved";
        Update update = TestUtils.getUpdate("errors_clear");

        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseMessage);

        PartialBotApiMethod<?> method = errors.parse(update);
        verify(errorService).clear();
        assertTrue(method instanceof SendMessage);

        SendMessage sendMessage = (SendMessage) method;
        assertEquals(expectedResponseMessage, sendMessage.getText());
    }

    @Test
    void getErrorDataWithWrongIdTest() {
        final String expectedExceptionMessage = "wrong id";
        Update update = TestUtils.getUpdate("errors_a");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedExceptionMessage);

        BotException botException = assertThrows(BotException.class, () -> errors.parse(update));
        assertEquals(expectedExceptionMessage, botException.getMessage());
    }

    @Test
    void getErrorDataOfNotExistenceErrorEntityTest() {
        final long errorId = 1;
        final String expectedExceptionMessage = "not existence Error";
        Update update = TestUtils.getUpdate("errors_" + errorId);

        when(errorService.get(errorId)).thenReturn(null);
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedExceptionMessage);

        BotException botException = assertThrows(BotException.class, () -> errors.parse(update));
        assertEquals(expectedExceptionMessage, botException.getMessage());
    }

    @Test
    void getErrorDataWithUnexpectedArgument() {
        Update update = TestUtils.getUpdate("errors abv");

        PartialBotApiMethod<?> method = assertDoesNotThrow(() -> errors.parse(update));
        assertNull(method);
    }

    @Test
    void getErrorDataTest() {
        final long errorId = 1;
        Update update = TestUtils.getUpdate("errors_" + errorId);
        Error error = new Error()
                .setId(errorId)
                .setDateTime(LocalDateTime.now())
                .setComment("comment")
                .setRequest("request")
                .setResponse("response")
                .setStacktrace("stacktrace");

        when(errorService.get(errorId)).thenReturn(error);

        PartialBotApiMethod<?> method = errors.parse(update);
        assertTrue(method instanceof SendDocument);

        SendDocument sendDocument = (SendDocument) method;
        assertNotNull(sendDocument.getDocument());
    }
}