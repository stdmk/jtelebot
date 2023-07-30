package org.telegram.bot.domain.commands;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        final String expectedText = "[Цитата #75286](http://bashorg.org/quote/75286)\n" +
                "*27 июня 2019 *\n" +
                "xxx: Я немного побаиваюсь людей, которые пишут мне в мессенджер\n" +
                "yyy: страшнее те, кто пишут на массажер или в утюг например\n" +
                "xxx: мне проще, я утюг от вай-фая отключил\n" +
                "yyy: А они по VPN ";
        Update update = TestUtils.getUpdateFromGroup(null);
        String rawRandomQuot = IOUtils.toString(
                new FileInputStream("src/test/java/org/telegram/bot/domain/commands/bash_random_quot.txt"), StandardCharsets.UTF_8);

        when(networkUtils.readStringFromURL(anyString(), any(Charset.class))).thenReturn(rawRandomQuot);

        SendMessage sendMessage = bash.parse(update);
        checkDefaultSendMessageParams(sendMessage, true, ParseMode.MARKDOWN);
        String actualText = sendMessage.getText();
        assertEquals(expectedText, actualText);
    }

    @Test
    void parseWithWrongInputTest() {
        Update update = TestUtils.getUpdateFromGroup("bash test");

        assertThrows(BotException.class, () -> bash.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseDefineQuotTest() throws IOException {
        final String expectedText = "[Цитата #1](http://bashorg.org/quote/1)\n" +
                "*25 ноября 2007*\n" +
                "Annette:\n" +
                "Недавно поставила новый антивирус - Avast. Оказалось, у него имеется одна интересная особенность: любит, понимаешь, \"поговорить\"  Так вот. Решила я пересмотреть Бриджит Джонс, мозги расслабить после трудного дня. И есть там сцена, где Хью Грант присаживается на край кровати и успокаивающе гладит Бриджит по макушке, попутно оправдываясь за то, что должен уехать. \n" +
                "Картина маслом: садится Хью Грант на кровать, проводит рукой Бриджит по голове, и одновременно с этим действием слышится характерный звук металлофона: \"Бррррррыньк!\"  Далее Хью Грант открывает рот и произносит с успокаивающим лицом: \"Вирусная база обновлена\". ";
        Update update = getUpdateFromGroup();
        update.getMessage().setText("bash 1");
        String rawDefinedQuot = IOUtils.toString(
                new FileInputStream("src/test/java/org/telegram/bot/domain/commands/bash_define_quot.txt"), StandardCharsets.UTF_8);

        when(networkUtils.readStringFromURL(anyString(), any(Charset.class))).thenReturn(rawDefinedQuot);

        SendMessage sendMessage = bash.parse(update);
        checkDefaultSendMessageParams(sendMessage, true, ParseMode.MARKDOWN);

        String actualText = sendMessage.getText();
        assertEquals(expectedText, actualText);
    }

    @Test
    void parseWithNoResponseTest() throws IOException {
        Update update = TestUtils.getUpdateFromGroup(null);

        when(networkUtils.readStringFromURL(anyString(), any(Charset.class))).thenThrow(new IOException());

        assertThrows(BotException.class, () -> bash.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithErrorsInResponseTest() throws IOException {
        final String errorText = "не имеют доступа для просмотра статей из данного раздела";
        Update update = getUpdateFromGroup();
        update.getMessage().setText("bash 1");
        String rawDefinedQuot = IOUtils.toString(
                new FileInputStream("src/test/java/org/telegram/bot/domain/commands/bash_define_quot.txt"), StandardCharsets.UTF_8) +
                errorText;

        when(networkUtils.readStringFromURL(anyString(), any(Charset.class))).thenReturn(rawDefinedQuot);

        assertThrows(BotException.class, () -> bash.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
    }
}