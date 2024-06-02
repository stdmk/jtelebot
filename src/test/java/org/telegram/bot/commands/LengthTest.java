package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LengthTest {

    @Mock
    private Bot bot;
    @Mock
    private BotStats botStats;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private NetworkUtils networkUtils;

    @InjectMocks
    private Length length;

    @Test
    void parseRequestWithoutTextAndDocumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("length");

        assertThrows(BotException.class, () -> length.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(commandWaitingService).getText(any(Message.class));
        verify(bot).sendTyping(anyLong());
    }

    @Test
    void parseRequestWithDocumentWithWrongMimeTypeTest() {
        Attachment attachment = TestUtils.getDocument();
        BotRequest request = TestUtils.getRequestFromGroup("length");
        request.getMessage().setAttachments(List.of(attachment));

        assertThrows(BotException.class, () -> length.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(commandWaitingService).getText(any(Message.class));
        verify(bot).sendTyping(anyLong());
    }

    @Test
    void parseRequestWithDocumentWithTelegramApiExceptionTest() {
        Attachment attachment = TestUtils.getDocument("text");
        BotRequest request = TestUtils.getRequestFromGroup("length");
        request.getMessage().setAttachments(List.of(attachment));

        when(bot.getFileFromTelegram(anyString())).thenThrow(new BotException("internal error"));

        assertThrows(BotException.class, () -> length.parse(request));
        verify(commandWaitingService).getText(any(Message.class));
        verify(bot).sendTyping(anyLong());
    }

    @Test
    void parseRequestWithDocumentTest() {
        final String fileContent = "test";
        final String expectedResponse = "${command.length.responselength} <b>" + fileContent.length() + "</b> ${command.length.symbols}";
        Attachment attachment = TestUtils.getDocument("application");
        BotRequest request = TestUtils.getRequestFromGroup("length");
        request.getMessage().setAttachments(List.of(attachment));

        when(bot.getFileFromTelegram(anyString())).thenReturn(fileContent.getBytes());

        BotResponse botResponse = length.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponse, textResponse.getText());
        verify(commandWaitingService).getText(any(Message.class));
        verify(bot).sendTyping(anyLong());
    }

    @Test
    void parseRequestWithTextParamTest() {
        final String textParam = "test";
        final String expectedResponse = "${command.length.responselength} <b>" + textParam.length() + "</b> ${command.length.symbols}";
        BotRequest request = TestUtils.getRequestFromGroup("length " + textParam);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());

        BotResponse botResponse = length.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponse, textResponse.getText());
        verify(commandWaitingService).getText(any(Message.class));
        verify(bot).sendTyping(anyLong());
    }

}