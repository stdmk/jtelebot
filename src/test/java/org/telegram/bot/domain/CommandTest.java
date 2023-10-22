package org.telegram.bot.domain;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.telegram.bot.Bot;
import org.telegram.bot.commands.Getid;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;

import static org.junit.jupiter.api.Assertions.*;

class CommandTest {

    @Mock
    private Bot bot;
    @Mock
    private UserService userService;
    @Mock
    private SpeechService speechService;

    @Test
    void cutCommandInTextTest() {
        String textWithSlash = "/bot how are you?";
        String textWithOnlyCommand = "bot";
        String textWithSlashAndBotUsername = "/news_1@jtelebot";
        String textCommon = "погода Ростов-на-Дону";

        Command<?> command = new Getid(bot, userService, speechService);

        assertEquals(command.cutCommandInText(textWithSlash), "how are you?");
        assertNull(command.cutCommandInText(textWithOnlyCommand));
        assertEquals(command.cutCommandInText(textWithSlashAndBotUsername), "_1");
        assertEquals(command.cutCommandInText(textCommon), "Ростов-на-Дону");
    }
}