package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatinTest {

    private final Latin latin = new Latin();

    @Test
    void parseWithoutArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("latin");
        List<BotResponse> botResponses = latin.parse(request);
        assertTrue(botResponses.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void parseTest(String argument, String expected) {
        BotRequest request = TestUtils.getRequestFromGroup("latin " + argument);
        BotResponse botResponse = latin.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expected, textResponse.getText());
    }

    private static Stream<Arguments> provideArguments() {
        return Stream.of(
                Arguments.of("hello all!", "hello all!\n\n${command.latin.nonlatincount}: 0"),
                Arguments.of("hеllo all!", "h→е←llo all!\n\n${command.latin.nonlatincount}: 1"),
                Arguments.of("hеllо аll!", "h→е←ll→о← →а←ll!\n\n${command.latin.nonlatincount}: 3"),
                Arguments.of("привет!", "→привет←!\n\n${command.latin.nonlatincount}: 6")
        );
    }

}