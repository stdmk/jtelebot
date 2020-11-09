package org.telegram.bot.domain;

import org.junit.jupiter.api.Test;
import org.telegram.bot.domain.commands.Getid;

import static org.junit.jupiter.api.Assertions.*;

class CommandParentTest {

    @Test
    void cutCommandInTextTest() {
        String textWithSlash = "/bot how are you?";
        String textWithOnlyCommand = "bot";
        String textWithSlashAndBotUsername = "/news_1@jtelebot";
        String textCommon = "погода Ростов-на-Дону";

        CommandParent<?> commandParent = new Getid();

        assertEquals(commandParent.cutCommandInText(textWithSlash), "how are you?");
        assertNull(commandParent.cutCommandInText(textWithOnlyCommand));
        assertEquals(commandParent.cutCommandInText(textWithSlashAndBotUsername), "_1");
        assertEquals(commandParent.cutCommandInText(textCommon), "Ростов-на-Дону");
    }
}