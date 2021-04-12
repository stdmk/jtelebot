package org.telegram.bot.domain.commands;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(classes = {TestConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Ignore
class BashTest {

    @MockBean
    SpeechService speechService;
    @MockBean
    NetworkUtils networkUtils;

    @Autowired
    Update update;
    @Autowired
    Update emptyTextMessageUpdate;

    String randomQuot;
    String defineQuot;
    Bash bashCommand;

    @BeforeAll
    void init() {
        try {
            randomQuot = IOUtils.toString( new FileInputStream("src/test/java/org/telegram/bot/domain/commands/bash_random_quot.txt"), StandardCharsets.UTF_8);
            defineQuot = IOUtils.toString( new FileInputStream("src/test/java/org/telegram/bot/domain/commands/bash_define_quot.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("cannot read bash quot files");
        }

        bashCommand = new Bash(speechService, networkUtils);
        update.getMessage().setText("bash 123");
    }

    @AfterAll
    void exit() {
        update = TestConfig.getCommonUpdate();
    }

    @Test
    void parseRandomBashQuotTest() {
        try {
            Mockito.when(networkUtils.readStringFromURL(any(String.class))).thenReturn(randomQuot);
        } catch (IOException e) {
            fail("cannot get bash random quot");
        }

        SendMessage sendMessage = null;
        try {
            sendMessage = bashCommand.parse(emptyTextMessageUpdate);
        } catch (Exception e) {
            fail("cannot parse bash random quot");
        }

        assertEquals("[Цитата #402288](http://bash.im/quote/402288)\n" +
                "_25.01.2009_\n" +
                "Ната: вчера подруга пропалила меня моей маме, рассказала, что я танцую с огнём, показала видюху, где я спьяну плюю керосином, а потом вешаюсь на какого то олуха\n" +
                "Ню: и что было, апокалипсис?!\n" +
                "Ната: почти. Я припёрлась домой и напоролась на озверевшую мать, которая прямо с порога начала на меня орать... я стою в полном шоке\n" +
                "Ню: а ты чего ожидала? единственная дочь, пьет, занимается экстремальными видами спорта и домогается до олухов. за это тебя по головке не погладят!\n" +
                "Ната: нет, я знала, что она промоет мне мозги... но не думала, что единственным поводом для скандала станет то, что на мне нет шапки...",
                sendMessage.getText());
    }

    @Test
    void parseDefineBashQuot() {
        try {
            Mockito.when(networkUtils.readStringFromURL(any(String.class))).thenReturn(defineQuot);
        } catch (IOException e) {
            fail("cannot get bash define quot");
        }

        SendMessage sendMessage = null;
        try {
            sendMessage = bashCommand.parse(update);
        } catch (Exception e) {
            fail("cannot parse bash random quot");
        }

        String quot = "[Цитата #123](http://bash.im/quote/123)\n" +
                "_ 02.05.2007 в  0:37\n" +
                "_\n" +
                " ximaera: свич они поместили в трансформаторную будку\n" +
                "ximaera: будки эти по весне заливает, я рассказывал\n" +
                "ximaera: смотришь в щель, там провода гудят, а в полуметре внизу от них вода\n" +
                "ximaera: ну проводам, очевидно, от воды похуй\n" +
                "ximaera: а свичу не совсем\n" +
                "ximaera: эти умники поместили его в пустое ведро\n" +
                "ximaera: чтоб он всплывал вместе с водой\n" +
                "ximaera: но трансформатор облюбовали птицы\n" +
                "ximaera: свили там себе гнездо\n" +
                "ximaera: и периодически усаживались передохнуть на это ведро\n" +
                "ximaera: вот блин, представь\n" +
                "ximaera: сидят люди в Интернете по сверхсовременным ноутбукам по вайфаю, наслаждаются вебдваноль\n" +
                "ximaera: 10 мбит, высокие технологии\n" +
                "ximaera: а между ними и Инетом в трансформаторной будке плавает в ржавом ведре засранный свич, вершина прогресса\n" +
                "          ";

        assertEquals("[Цитата #123](http://bash.im/quote/123)\n" +
                "_ 02.05.2007 в  0:37\r" +
                "_\n" +
                " ximaera: свич они поместили в трансформаторную будку\n" +
                "ximaera: будки эти по весне заливает, я рассказывал\n" +
                "ximaera: смотришь в щель, там провода гудят, а в полуметре внизу от них вода\n" +
                "ximaera: ну проводам, очевидно, от воды похуй\n" +
                "ximaera: а свичу не совсем\n" +
                "ximaera: эти умники поместили его в пустое ведро\n" +
                "ximaera: чтоб он всплывал вместе с водой\n" +
                "ximaera: но трансформатор облюбовали птицы\n" +
                "ximaera: свили там себе гнездо\n" +
                "ximaera: и периодически усаживались передохнуть на это ведро\n" +
                "ximaera: вот блин, представь\n" +
                "ximaera: сидят люди в Интернете по сверхсовременным ноутбукам по вайфаю, наслаждаются вебдваноль\n" +
                "ximaera: 10 мбит, высокие технологии\n" +
                "ximaera: а между ними и Инетом в трансформаторной будке плавает в ржавом ведре засранный свич, вершина прогресса\r\n" +
                "          ", sendMessage.getText());
    }
}