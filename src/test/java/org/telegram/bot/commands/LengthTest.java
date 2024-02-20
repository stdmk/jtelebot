package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

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
    void parseUpdateWithoutTextAndDocumentTest() {
        Update update = TestUtils.getUpdateFromGroup("length");

        assertThrows(BotException.class, () -> length.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(commandWaitingService).getText(any(Message.class));
        verify(bot).sendTyping(anyLong());
    }

    @Test
    void parseUpdateWithDocumentWithWrongMimeTypeTest() {
        Document document = TestUtils.getDocument();
        Update update = TestUtils.getUpdateFromGroup("length");
        update.getMessage().setDocument(document);

        assertThrows(BotException.class, () -> length.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(commandWaitingService).getText(any(Message.class));
        verify(bot).sendTyping(anyLong());
    }

    @Test
    void parseUpdateWithDocumentWithTelegramApiExceptionTest() throws TelegramApiException, IOException {
        Document document = TestUtils.getDocument();
        document.setMimeType("text");
        Update update = TestUtils.getUpdateFromGroup("length");
        update.getMessage().setDocument(document);

        when(networkUtils.getFileFromTelegram(anyString())).thenThrow(new TelegramApiException());

        assertThrows(BotException.class, () -> length.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
        verify(botStats).incrementErrors(any(Document.class), any(Throwable.class), anyString());
        verify(commandWaitingService).getText(any(Message.class));
        verify(bot).sendTyping(anyLong());
    }

    @Test
    void parseUpdateWithDocumentTest() throws TelegramApiException, IOException {
        final String fileContent = "test";
        final String expectedResponse = "${command.length.responselength} <b>" + fileContent.length() + "</b> ${command.length.symbols}";
        Document document = TestUtils.getDocument();
        document.setMimeType("application");
        Update update = TestUtils.getUpdateFromGroup("length");
        update.getMessage().setDocument(document);

        when(networkUtils.getFileFromTelegram(anyString())).thenReturn(fileContent.getBytes());

        SendMessage sendMessage = length.parse(update).get(0);
        TestUtils.checkDefaultSendMessageParams(sendMessage);
        assertEquals(expectedResponse, sendMessage.getText());
        verify(commandWaitingService).getText(any(Message.class));
        verify(bot).sendTyping(anyLong());
    }

    @Test
    void parseUpdateWithTextParamTest() {
        final String textParam = "test";
        final String expectedResponse = "${command.length.responselength} <b>" + textParam.length() + "</b> ${command.length.symbols}";
        Update update = TestUtils.getUpdateFromGroup("length " + textParam);

        SendMessage sendMessage = length.parse(update).get(0);
        TestUtils.checkDefaultSendMessageParams(sendMessage);
        assertEquals(expectedResponse, sendMessage.getText());
        verify(commandWaitingService).getText(any(Message.class));
        verify(bot).sendTyping(anyLong());
    }

}