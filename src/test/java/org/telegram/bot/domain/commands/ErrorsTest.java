package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Error;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ErrorService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

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
        Update update = getUpdateFromGroup("errors");
        List<Error> errorList = List.of(new Error().setId(1L).setDateTime(LocalDateTime.now()).setComment("comment"));

        when(errorService.getAll()).thenReturn(errorList);

        PartialBotApiMethod<?> method = errors.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        checkDefaultSendMessageParams(method);
    }

    @Test
    void clearErrorListTest() {
        final String expectedResponseMessage = "saved";
        Update update = getUpdateFromGroup("errors_clear");

        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseMessage);

        PartialBotApiMethod<?> method = errors.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(errorService).clear();
        SendMessage sendMessage = checkDefaultSendMessageParams(method);
        assertEquals(expectedResponseMessage, sendMessage.getText());
    }

    @Test
    void getErrorDataWithWrongIdTest() {
        final String expectedExceptionMessage = "wrong id";
        Update update = getUpdateFromGroup("errors_a");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedExceptionMessage);

        BotException botException = assertThrows(BotException.class, () -> errors.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        assertEquals(expectedExceptionMessage, botException.getMessage());
    }

    @Test
    void getErrorDataOfNotExistenceErrorEntityTest() {
        final long errorId = 1;
        final String expectedExceptionMessage = "not existence Error";
        Update update = getUpdateFromGroup("errors_" + errorId);

        when(errorService.get(errorId)).thenReturn(null);
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedExceptionMessage);

        BotException botException = assertThrows(BotException.class, () -> errors.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        assertEquals(expectedExceptionMessage, botException.getMessage());
    }

    @Test
    void getErrorDataWithUnexpectedArgument() {
        Update update = getUpdateFromGroup("errors abv");
        PartialBotApiMethod<?> method = assertDoesNotThrow(() -> errors.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        assertNull(method);
    }

    @Test
    void getErrorDataTest() {
        final long errorId = 1;
        Update update = getUpdateFromGroup("errors_" + errorId);
        Error error = new Error()
                .setId(errorId)
                .setDateTime(LocalDateTime.now())
                .setComment("comment")
                .setRequest("request")
                .setResponse("response")
                .setStacktrace("stacktrace");

        when(errorService.get(errorId)).thenReturn(error);

        PartialBotApiMethod<?> method = errors.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        checkDefaultSendDocumentParams(method);
    }
}