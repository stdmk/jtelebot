package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LengthTest {

    @Mock
    private Bot bot;
    @Mock
    private BotStats botStats;
    @Mock
    private SpeechService speechService;

    @InjectMocks
    private Length length;

    @Test
    void parseRequestWithoutTextAndDocumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("length");

        assertThrows(BotException.class, () -> length.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(anyLong());
    }

    @Test
    void parseRequestWithDocumentWithWrongMimeTypeTest() {
        Attachment attachment = TestUtils.getDocument();
        BotRequest request = TestUtils.getRequestFromGroup("length");
        request.getMessage().setAttachments(List.of(attachment));

        assertThrows(BotException.class, () -> length.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(anyLong());
    }

    @Test
    void parseRequestWithDocumentWithTelegramApiExceptionTest() throws TelegramApiException, IOException {
        Attachment attachment = TestUtils.getDocument("text");
        BotRequest request = TestUtils.getRequestFromGroup("length");
        request.getMessage().setAttachments(List.of(attachment));

        when(bot.getInputStreamFromTelegramFile(anyString())).thenThrow(new BotException("internal error"));

        assertThrows(BotException.class, () -> length.parse(request));
        verify(bot).sendTyping(anyLong());
    }

    @ParameterizedTest
    @MethodSource("provideTelegramExceptions")
    void parseRequestWithTelegramExceptionTest(Exception exception) throws TelegramApiException, IOException {
        Attachment attachment = TestUtils.getDocument();
        attachment.setMimeType("text");
        BotRequest request = TestUtils.getRequestFromGroup("length");
        request.getMessage().setAttachments(List.of(attachment));

        when(bot.getInputStreamFromTelegramFile(anyString())).thenThrow(exception);

        assertThrows((BotException.class), () -> length.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
        verify(botStats).incrementErrors(request, exception, "Failed to get file from telegram");
        verify(bot).sendTyping(TestUtils.DEFAULT_CHAT_ID);
    }

    private static Stream<Exception> provideTelegramExceptions() {
        return Stream.of(
                new TelegramApiException(""),
                new IOException("")
        );
    }

    @Test
    void parseRequestWithDocumentTest() throws TelegramApiException, IOException {
        final String fileContent = "test";
        final String expectedResponse = "${command.length.responselength} <b>" + fileContent.length() + "</b> ${command.length.symbols}";
        Attachment attachment = TestUtils.getDocument("application");
        BotRequest request = TestUtils.getRequestFromGroup("length");
        request.getMessage().setAttachments(List.of(attachment));

        InputStream inputStream = mock(InputStream.class);
        when(inputStream.readAllBytes()).thenReturn(fileContent.getBytes());
        when(bot.getInputStreamFromTelegramFile(anyString())).thenReturn(inputStream);

        BotResponse botResponse = length.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponse, textResponse.getText());
        verify(bot).sendTyping(anyLong());
    }

    @Test
    void parseRequestWithTextParamTest() {
        final String textParam = "test";
        final String expectedResponse = "${command.length.responselength} <b>" + textParam.length() + "</b> ${command.length.symbols}";
        BotRequest request = TestUtils.getRequestFromGroup("length " + textParam);

        BotResponse botResponse = length.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponse, textResponse.getText());
        verify(bot).sendTyping(anyLong());
    }

}