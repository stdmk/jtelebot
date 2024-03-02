package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.telegram.bot.TestUtils.checkDefaultSendMessageParams;
import static org.telegram.bot.TestUtils.getUpdateFromGroup;

@ExtendWith(MockitoExtension.class)
class BashTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;
    @Mock
    private NetworkUtils networkUtils;

    @InjectMocks
    private Bash bash;

    @Test
    void parseRandomQuotTest() throws IOException {
        final String expectedText = "[Цитата #10229](https://xn--80abh7bk0c.xn--p1ai/quote/10229)\n" +
                "*06.02.2006 в 10:25*\n" +
                "<yМHuK> Bоnpoс #276: Трaдиционный русский напиток  5 букв\n" +
                "<Chrono> водка\n" +
                "<LSD> водка\n" +
                "<Racco^n> водка\n" +
                "<LD> водка";
        Update update = TestUtils.getUpdateFromGroup(null);
        String rawRandomQuot = TestUtils.getResourceAsString("bash/bash_random_quote.txt");

        when(networkUtils.readStringFromURL(anyString())).thenReturn(rawRandomQuot);

        SendMessage sendMessage = bash.parse(update).get(0);
        verify(bot).sendTyping(update.getMessage().getChatId());
        checkDefaultSendMessageParams(sendMessage, true, ParseMode.MARKDOWN);
        String actualText = sendMessage.getText();
        assertEquals(expectedText, actualText);
    }

    @Test
    void parseWithWrongInputTest() {
        Update update = TestUtils.getUpdateFromGroup("bash test");

        assertThrows(BotException.class, () -> bash.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseDefineQuotTest() throws IOException {
        final String expectedText = "[Цитата #10229](https://xn--80abh7bk0c.xn--p1ai/quote/10229)\n" +
                "*06.02.2006 в 10:25*\n" +
                "<yМHuK> Bоnpoс #276: Трaдиционный русский напиток  5 букв\n" +
                "<Chrono> водка\n" +
                "<LSD> водка\n" +
                "<Racco^n> водка\n" +
                "<LD> водка";
        Update update = getUpdateFromGroup();
        update.getMessage().setText("bash 10229");
        String rawDefinedQuot = TestUtils.getResourceAsString("bash/bash_define_quote.txt");

        when(networkUtils.readStringFromURL(anyString())).thenReturn(rawDefinedQuot);

        SendMessage sendMessage = bash.parse(update).get(0);
        verify(bot).sendTyping(update.getMessage().getChatId());
        checkDefaultSendMessageParams(sendMessage, true, ParseMode.MARKDOWN);

        String actualText = sendMessage.getText();
        assertEquals(expectedText, actualText);
    }

    @Test
    void parseWithNoResponseTest() throws IOException {
        Update update = TestUtils.getUpdateFromGroup(null);

        when(networkUtils.readStringFromURL(anyString())).thenThrow(new IOException());

        assertThrows(BotException.class, () -> bash.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithErrorsInResponseTest() throws IOException {
        Update update = getUpdateFromGroup();
        update.getMessage().setText("bash 1");
        String rawDefinedQuot = TestUtils.getResourceAsString("bash/bash_not_found_quote.txt");

        when(networkUtils.readStringFromURL(anyString())).thenReturn(rawDefinedQuot);

        assertThrows(BotException.class, () -> bash.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
    }
}