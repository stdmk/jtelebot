package org.telegram.bot.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.telegram.bot.utils.TextUtils.*;

class TextUtilsTest {

    @org.junit.jupiter.api.Test
    void reduceSpacesTest() {
        String textExample = "test\n\n\n\ntest1    test2";

        assertEquals("test\ntest1 test2", TextUtils.reduceSpaces(textExample));
    }

    @Test
    void getPotentialCommandInTextTest() {
        String textWithSlash = "/bot";
        String textWithSlashAndUnderline = "/bot_34";
        String textWithSlashAndCyphers = "/bot432bot432 rgeg";
        String textRussian = "Бот, как дела";
        String commonText = "погода Ростов-на-Дону";
        String commonTextWithCyphers = "погода3 Ростов-на-Дону";

        assertEquals("bot", getPotentialCommandInText(textWithSlash));
        assertEquals("bot_34", getPotentialCommandInText(textWithSlashAndUnderline));
        assertEquals("bot432bot432", getPotentialCommandInText(textWithSlashAndCyphers));
        assertEquals("бот", getPotentialCommandInText(textRussian));
        assertEquals("погода", getPotentialCommandInText(commonText));
        assertEquals("погода3", getPotentialCommandInText(commonTextWithCyphers));
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
}