package org.telegram.bot.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeDownloadingTest {

    @Mock
    private Bot bot;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private InternationalizationService internationalizationService;

    @InjectMocks
    private TimeDownloading timeDownloading;

    @BeforeEach
    void init() {
        Map<Long, Set<String>> weightNamesMultiplierMap = (Map<Long, Set<String>>) ReflectionTestUtils.getField(timeDownloading, "weightNamesMultiplierMap");
        weightNamesMultiplierMap.put(1L, Set.of("b", "byte"));
        weightNamesMultiplierMap.put(1024L, Set.of("kb", "kilobyte"));
        weightNamesMultiplierMap.put(1024L * 1024L, Set.of("mb", "megabyte"));
        weightNamesMultiplierMap.put(1024L * 1024L * 1024L, Set.of("gb", "gigabyte"));
        weightNamesMultiplierMap.put(1024L * 1024L * 1024L * 1024L, Set.of("tb", "terabyte"));
        weightNamesMultiplierMap.put(1024L * 1024L * 1024L * 1024L * 1024L, Set.of("pb", "petabyte"));

        Map<Long, Set<String>> speedNamesMultiplierMap = (Map<Long, Set<String>>) ReflectionTestUtils.getField(timeDownloading, "speedNamesMultiplierMap");
        speedNamesMultiplierMap.put(1L, Set.of("bit"));
        speedNamesMultiplierMap.put(1024L, Set.of("kbit", "kilobit"));
        speedNamesMultiplierMap.put(1024L * 1024L, Set.of("mbit", "megabit"));
        speedNamesMultiplierMap.put(1024L * 1024L * 1024L, Set.of("gbit", "gigabit"));
        speedNamesMultiplierMap.put(1024L * 1024L * 1024L * 1024L, Set.of("tbit", "terabit"));
        speedNamesMultiplierMap.put(1024L * 1024L * 1024L * 1024L * 1024L, Set.of("pbit", "petabit"));
    }

    @Test
    void parseWithoutArgumentsTest() {
        final String expectedResponseText = "${command.timedownloading.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup("timedownloading");

        BotResponse botResponse = timeDownloading.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(commandWaitingService).add(request.getMessage(), TimeDownloading.class);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"test", "a b", "1.3 a b", "1,3 gb b bb", "1.3 gb 50 gg"})
    void parseWithWrongArgumentsTest(String arguments) {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("timedownloading " + arguments);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> timeDownloading.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
        verify(bot).sendTyping(message.getChatId());
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void parseTest(String arguments, String expected) {
        BotRequest request = TestUtils.getRequestFromGroup("timedownloading " + arguments);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());

        BotResponse botResponse = timeDownloading.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expected, textResponse.getText());
        verify(bot).sendTyping(message.getChatId());
    }

    private static Stream<Arguments> provideArguments() {
        return Stream.of(
                Arguments.of("1 b 1000 mbit", "${command.timedownloading.file} *1,00 b* ${command.timedownloading.willdownload} *${command.timedownloading.instantly}*"),
                Arguments.of("1 gb 50 mbit", "${command.timedownloading.file} *1024,00 Mb* ${command.timedownloading.willdownload} ${command.timedownloading.in} *2 ${utils.date.m}. 43 ${utils.date.s}. *")
        );
    }

}