package org.telegram.bot.mapper.telegram.response;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.telegram.bot.enums.FormattingStyle;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParseModeMapperTest {

    private final ParseModeMapper parseModeMapper = new ParseModeMapper();

    @ParameterizedTest
    @MethodSource("provideFormattingStyles")
    void toParseMode(FormattingStyle formattingStyle, String expected) {
        String actual = parseModeMapper.toParseMode(formattingStyle);
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> provideFormattingStyles() {
        return Stream.of(
            Arguments.of(FormattingStyle.HTML, "html"),
            Arguments.of(FormattingStyle.MARKDOWN, "Markdown"),
            Arguments.of(FormattingStyle.MARKDOWN2, "MarkdownV2")
        );
    }

}