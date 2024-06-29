package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RandomTest {

    @Mock
    private SpeechService speechService;

    @InjectMocks
    private Random random;

    @Test
    void parseWithoutArgumentsTest() {
        final String heads = "${command.random.heads}";
        final String tails = "${command.random.tails}";
        BotRequest request = TestUtils.getRequestFromGroup("random");

        List<List<BotResponse>> responseList = IntStream.range(1, 1000).mapToObj(i -> random.parse(request)).toList();

        assertTrue(responseList.stream().noneMatch(response -> {
            String responseText = ((TextResponse) response.get(0)).getText();
            return !heads.equals(responseText) && !tails.equals(responseText);
        }));

        assertTrue(responseList.stream().anyMatch(response ->  heads.equals(((TextResponse) response.get(0)).getText())));
        assertTrue(responseList.stream().anyMatch(response ->  tails.equals(((TextResponse) response.get(0)).getText())));

        TestUtils.checkDefaultTextResponseParams(responseList.get(0).get(0));
    }

    @Test
    void parseWithOneTextArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("random test");
        assertThrows((BotException.class), () -> random.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWithZeroAsArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("random 0");

        BotResponse botResponse = random.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        long randomValue = Long.parseLong(textResponse.getText());
        assertTrue(randomValue >= 0);
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE})
    void parseWithOneIntArgumentTest(Long argument) {
        BotRequest request = TestUtils.getRequestFromGroup("random " + argument);

        BotResponse botResponse = random.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        long randomValue = Long.parseLong(textResponse.getText());
        if (argument < 0) {
            assertTrue(randomValue <= 0);
        } else {
            assertTrue(randomValue >= 0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"test 1", "1 test"})
    void parseWithCorruptedRangeAsArgumentTest(String range) {
        BotRequest request = TestUtils.getRequestFromGroup("random " + range);
        String[] arguments = range.split(" ");

        BotResponse botResponse = random.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        String responseText = textResponse.getText();
        assertTrue(arguments[0].equals(responseText) || arguments[1].equals(responseText));
    }

    @Test
    void parseWithRangeAsArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("random " + Long.MIN_VALUE + " " + Long.MAX_VALUE);

        BotResponse botResponse = random.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertDoesNotThrow(() -> Long.parseLong(textResponse.getText()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"test1 test2 test3", "1 2 3 4", "test1 1 test2 2 test3 3"})
    void parseWithElementListAsArgumentTest(String elements) {
        BotRequest request = TestUtils.getRequestFromGroup("random " + elements);
        List<String> arguments = Arrays.stream(elements.split(" ")).toList();

        List<List<BotResponse>> responseList = IntStream.range(1, 1000).mapToObj(i -> random.parse(request)).toList();
        List<String> responseTextList = responseList.stream().map(responseList1 -> responseList1.get(0)).map(response -> ((TextResponse) response).getText()).toList();

        assertTrue(responseTextList.containsAll(arguments));

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(responseList.get(0).get(0));
        assertTrue(arguments.contains(textResponse.getText()));

    }

}