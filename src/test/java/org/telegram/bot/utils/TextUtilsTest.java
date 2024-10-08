package org.telegram.bot.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.telegram.bot.utils.TextUtils.*;

class TextUtilsTest {

    @Test
    void reduceSpacesTest() {
        String textExample = "test\n\n\n\ntest1    test2";

        assertEquals("test\ntest1 test2", TextUtils.reduceSpaces(textExample));
    }

    @Test
    void getPotentialCommandInTextTest() {
        String textWithSlash = "/bot";
        String textRussian = "Бот, как дела";
        String commonText = "погода Ростов-на-Дону";

        assertEquals("bot", getPotentialCommandInText(textWithSlash));
        assertEquals("бот", getPotentialCommandInText(textRussian));
        assertEquals("погода", getPotentialCommandInText(commonText));
    }

    @Test
    void cutMarkdownSymbolsInTextTest() {
        String text = "*test1* _test2_ `test3` [test4](test5)";
        assertEquals("test1 test2 test3 test4test5", cutMarkdownSymbolsInText(text));
    }

    @Test
    void cutHtmlTagsTest() {
        String text = "<a href=\"example.com\">test</a>";
        assertEquals("test", cutHtmlTags(text));
    }

    @Test
    void isThatNotIntegerTest() {
        assertFalse(isThatPositiveInteger("text"));
        assertFalse(isThatPositiveInteger("1.1"));
        assertFalse(isThatPositiveInteger("1,2"));
        assertFalse(isThatPositiveInteger(""));
        assertFalse(isThatPositiveInteger(null));
        assertTrue(isThatPositiveInteger("1"));
    }

    @Test
    void cutCommandInTextTest() {
        String textWithSlash = "/bot how are you?";
        String textWithOnlyCommand = "bot";
        String textWithSlashAndBotUsername = "/news_1@jtelebot";
        String textCommon = "погода Ростов-на-Дону";

        assertEquals("how are you?", TextUtils.cutCommandInText(textWithSlash));
        assertNull(TextUtils.cutCommandInText(textWithOnlyCommand));
        assertEquals("_1", TextUtils.cutCommandInText(textWithSlashAndBotUsername));
        assertEquals("Ростов-на-Дону", TextUtils.cutCommandInText(textCommon));
    }

}