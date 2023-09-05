package org.telegram.bot.domain;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.commands.Getid;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;

import static org.junit.jupiter.api.Assertions.*;

class CommandParentTest {

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

        CommandParent<?> commandParent = new Getid(bot, userService, speechService);

        assertEquals(commandParent.cutCommandInText(textWithSlash), "how are you?");
        assertNull(commandParent.cutCommandInText(textWithOnlyCommand));
        assertEquals(commandParent.cutCommandInText(textWithSlashAndBotUsername), "_1");
        assertEquals(commandParent.cutCommandInText(textCommon), "Ростов-на-Дону");
    }
}