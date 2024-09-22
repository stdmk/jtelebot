package org.telegram.bot.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import org.telegram.bot.services.SpeechService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimeDeltaTest {

    private static final LocalDate CURRENT_DATE = LocalDate.of(2000, 1, 1);

    @Mock
    private Bot bot;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private Clock clock;

    @InjectMocks
    private TimeDelta timeDelta;

    @BeforeEach
    void init() {
        ReflectionTestUtils.invokeMethod(timeDelta, "postConstruct");
    }

    @Test
    void parseWithoutArgumentsTest() {
        final String expectedResponseText = "${command.timedelta.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup();

        BotResponse botResponse = timeDelta.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @ParameterizedTest
    @MethodSource("provideWrongArguments")
    void parseWrongArgumentTest(String argument) {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("timedelta " + argument);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> timeDelta.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    private static Stream<String> provideWrongArguments() {
        return Stream.of(
                " ",
                "35.03.2024 45:03:25",
                "35.03",
                "45:03"
        );
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void parseTest(String argument, String expectedResponse) {
        BotRequest request = TestUtils.getRequestFromGroup("timedelta " + argument);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        BotResponse botResponse = timeDelta.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponse, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

    private static Stream<Arguments> provideArguments() {
        return Stream.of(
                Arguments.of("01.01.2000 00:00:00 02.03 01:02:03",      "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2000 01:02:03:*\n61 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}. 3 ${utils.date.s}.  (2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01.01.2000 00:00:00 02.03.2004 01:02:03", "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2004 01:02:03:*\n1522 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}. 3 ${utils.date.s}.  (4 ${utils.date.year}. 2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01.01.2000 00:00:00 02.03.2004 01:02",    "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2004 01:02:00:*\n1522 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}.  (4 ${utils.date.year}. 2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01.01.2000 00:00:00 02.03 01:02",         "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2000 01:02:00:*\n61 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}.  (2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01.01.2000 00:00:00 02.03.2004",          "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2004 00:00:00:*\n1522 ${utils.date.d}.  (4 ${utils.date.year}. 2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01.01.2000 00:00:00 02.03",               "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2000 00:00:00:*\n61 ${utils.date.d}.  (2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01.01.2000 00:00:00 01:02:03",            "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 01.01.2000 01:02:03:*\n1 ${utils.date.h}. 2 ${utils.date.m}. 3 ${utils.date.s}. *"),
                Arguments.of("01.01.2000 00:00:00 01:02",               "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 01.01.2000 01:02:00:*\n1 ${utils.date.h}. 2 ${utils.date.m}. *"),
                Arguments.of("01.01.2000 00:00 02.03.2004 01:02:03",    "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2004 01:02:03:*\n1522 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}. 3 ${utils.date.s}.  (4 ${utils.date.year}. 2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01.01 00:00:00 02.03.2004 01:02:03",      "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2004 01:02:03:*\n1522 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}. 3 ${utils.date.s}.  (4 ${utils.date.year}. 2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01.01 00:00 02.03.2004 01:02:03",         "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2004 01:02:03:*\n1522 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}. 3 ${utils.date.s}.  (4 ${utils.date.year}. 2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01.01.2000 02.03.2004 01:02:03",          "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2004 01:02:03:*\n1522 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}. 3 ${utils.date.s}.  (4 ${utils.date.year}. 2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01.01 02.03.2004 01:02:03",               "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2004 01:02:03:*\n1522 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}. 3 ${utils.date.s}.  (4 ${utils.date.year}. 2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("00:00:00 02.03.2004 01:02:03",            "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2004 01:02:03:*\n1522 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}. 3 ${utils.date.s}.  (4 ${utils.date.year}. 2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("00:00 02.03.2004 01:02:03",               "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2004 01:02:03:*\n1522 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}. 3 ${utils.date.s}.  (4 ${utils.date.year}. 2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01.01.2000 00:00 02.03.2004 01:02",       "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2004 01:02:00:*\n1522 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}.  (4 ${utils.date.year}. 2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01.01 00:00:00 02.03 01:02:03",           "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2000 01:02:03:*\n61 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}. 3 ${utils.date.s}.  (2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01.01 00:00 02.03 01:02",                 "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2000 01:02:00:*\n61 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}.  (2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01.01.2000 02.03.2004",                   "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2004 00:00:00:*\n1522 ${utils.date.d}.  (4 ${utils.date.year}. 2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01.01 02.03",                             "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2000 00:00:00:*\n61 ${utils.date.d}.  (2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("00:00:00 01:02:03",                       "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 01.01.2000 01:02:03:*\n1 ${utils.date.h}. 2 ${utils.date.m}. 3 ${utils.date.s}. *"),
                Arguments.of("00:00 01:02",                             "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 01.01.2000 01:02:00:*\n1 ${utils.date.h}. 2 ${utils.date.m}. *"),
                Arguments.of("01.01.2000 00:00:00 50",                  "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 20.02.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 19 ${utils.date.d}. )*"),
                Arguments.of("01.01.2000 00:00 50",                     "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 20.02.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 19 ${utils.date.d}. )*"),
                Arguments.of("01.01 00:00:00 50",                       "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 20.02.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 19 ${utils.date.d}. )*"),
                Arguments.of("01.01 00:00 50",                          "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 20.02.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 19 ${utils.date.d}. )*"),
                Arguments.of("01.01.2000 50",                           "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 20.02.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 19 ${utils.date.d}. )*"),
                Arguments.of("01.01 50",                                "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 20.02.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 19 ${utils.date.d}. )*"),
                Arguments.of("00:00:00 50",                             "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 20.02.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 19 ${utils.date.d}. )*"),
                Arguments.of("00:00 50",                                "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 20.02.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 19 ${utils.date.d}. )*"),
                Arguments.of("01.01.2000 00:00:00 -50",                 "${command.timedelta.from} 12.11.1999 00:00:00 ${command.timedelta.to} 01.01.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 20 ${utils.date.d}. )*"),
                Arguments.of("01.01.2000 00:00 -50",                    "${command.timedelta.from} 12.11.1999 00:00:00 ${command.timedelta.to} 01.01.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 20 ${utils.date.d}. )*"),
                Arguments.of("01.01 00:00:00 -50",                      "${command.timedelta.from} 12.11.1999 00:00:00 ${command.timedelta.to} 01.01.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 20 ${utils.date.d}. )*"),
                Arguments.of("01.01 00:00 -50",                         "${command.timedelta.from} 12.11.1999 00:00:00 ${command.timedelta.to} 01.01.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 20 ${utils.date.d}. )*"),
                Arguments.of("01.01.2000 -50",                          "${command.timedelta.from} 12.11.1999 00:00:00 ${command.timedelta.to} 01.01.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 20 ${utils.date.d}. )*"),
                Arguments.of("01.01 -50",                               "${command.timedelta.from} 12.11.1999 00:00:00 ${command.timedelta.to} 01.01.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 20 ${utils.date.d}. )*"),
                Arguments.of("00:00:00 -50",                            "${command.timedelta.from} 12.11.1999 00:00:00 ${command.timedelta.to} 01.01.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 20 ${utils.date.d}. )*"),
                Arguments.of("00:00 -50",                               "${command.timedelta.from} 12.11.1999 00:00:00 ${command.timedelta.to} 01.01.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 20 ${utils.date.d}. )*"),
                Arguments.of("02.03 01:02:03",                          "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2000 01:02:03:*\n61 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}. 3 ${utils.date.s}.  (2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("02.03.2004 01:02:03",                     "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2004 01:02:03:*\n1522 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}. 3 ${utils.date.s}.  (4 ${utils.date.year}. 2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("02.03.2004 01:02",                        "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2004 01:02:00:*\n1522 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}.  (4 ${utils.date.year}. 2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("02.03 01:02",                             "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2000 01:02:00:*\n61 ${utils.date.d}. 1 ${utils.date.h}. 2 ${utils.date.m}.  (2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("02.03.2004",                              "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2004 00:00:00:*\n1522 ${utils.date.d}.  (4 ${utils.date.year}. 2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("02.03",                                   "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 02.03.2000 00:00:00:*\n61 ${utils.date.d}.  (2 ${utils.date.months}. 1 ${utils.date.d}. )*"),
                Arguments.of("01:02:03",                                "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 01.01.2000 01:02:03:*\n1 ${utils.date.h}. 2 ${utils.date.m}. 3 ${utils.date.s}. *"),
                Arguments.of("01:02",                                   "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 01.01.2000 01:02:00:*\n1 ${utils.date.h}. 2 ${utils.date.m}. *"),
                Arguments.of("50",                                      "${command.timedelta.from} 01.01.2000 00:00:00 ${command.timedelta.to} 20.02.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 19 ${utils.date.d}. )*"),
                Arguments.of("-50",                                     "${command.timedelta.from} 12.11.1999 00:00:00 ${command.timedelta.to} 01.01.2000 00:00:00:*\n50 ${utils.date.d}.  (1 ${utils.date.months}. 20 ${utils.date.d}. )*")
        );
    }

}