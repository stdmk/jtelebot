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
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.telegram.bot.TestUtils.checkDefaultTextResponseParams;

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
        final String expectedText = """
                [Цитата #10229](https://xn--80abh7bk0c.xn--p1ai/quote/10229)
                *06.02.2006 в 10:25*
                <yМHuK> Bоnpoс #276: Трaдиционный русский напиток  5 букв
                <Chrono> водка
                <LSD> водка
                <Racco^n> водка
                <LD> водка""";
        BotRequest request = TestUtils.getRequestFromGroup(null);
        String rawRandomQuot = TestUtils.getResourceAsString("bash/bash_random_quote.txt");

        when(networkUtils.readStringFromURL(anyString())).thenReturn(rawRandomQuot);

        BotResponse botResponse = bash.parse(request).get(0);
        TextResponse textResponse = checkDefaultTextResponseParams(botResponse);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(textResponse, true, FormattingStyle.MARKDOWN);
        String actualText = textResponse.getText();
        assertEquals(expectedText, actualText);
    }

    @Test
    void parseWithWrongInputTest() {
        BotRequest request = TestUtils.getRequestFromGroup("bash test");

        assertThrows(BotException.class, () -> bash.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseDefineQuotTest() throws IOException {
        final String expectedText = """
                [Цитата #10229](https://xn--80abh7bk0c.xn--p1ai/quote/10229)
                *06.02.2006 в 10:25*
                <yМHuK> Bоnpoс #276: Трaдиционный русский напиток  5 букв
                <Chrono> водка
                <LSD> водка
                <Racco^n> водка
                <LD> водка""";
        BotRequest request = TestUtils.getRequestFromGroup();
        request.getMessage().setText("bash 10229");
        String rawDefinedQuot = TestUtils.getResourceAsString("bash/bash_define_quote.txt");

        when(networkUtils.readStringFromURL(anyString())).thenReturn(rawDefinedQuot);

        BotResponse botResponse = bash.parse(request).get(0);
        TextResponse textResponse = checkDefaultTextResponseParams(botResponse);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(textResponse, true, FormattingStyle.MARKDOWN);

        String actualText = textResponse.getText();
        assertEquals(expectedText, actualText);
    }

    @Test
    void parseWithNoResponseTest() throws IOException {
        BotRequest request = TestUtils.getRequestFromGroup(null);

        when(networkUtils.readStringFromURL(anyString())).thenThrow(new IOException());

        assertThrows(BotException.class, () -> bash.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithErrorsInResponseTest() throws IOException {
        BotRequest request = TestUtils.getRequestFromGroup();
        request.getMessage().setText("bash 1");
        String rawDefinedQuot = TestUtils.getResourceAsString("bash/bash_not_found_quote.txt");

        when(networkUtils.readStringFromURL(anyString())).thenReturn(rawDefinedQuot);

        assertThrows(BotException.class, () -> bash.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
    }
}