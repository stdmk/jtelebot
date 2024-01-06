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
        assertFalse(isThatInteger("text"));
        assertFalse(isThatInteger("1.1"));
        assertFalse(isThatInteger("1,2"));
        assertFalse(isThatInteger(""));
        assertFalse(isThatInteger(null));
        assertTrue(isThatInteger("1"));
    }
}