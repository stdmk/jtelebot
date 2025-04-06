package org.telegram.bot.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.UnitUtils;
import org.telegram.bot.commands.convertors.LengthUnit;
import org.telegram.bot.commands.convertors.TimeUnit;
import org.telegram.bot.commands.convertors.Unit;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConverterTest {

    private final Bot bot = mock(Bot.class);
    private final SpeechService speechService = mock(SpeechService.class);
    private final InternationalizationService internationalizationService = mock(InternationalizationService.class);
    private final List<Unit> units = List.of(
            new TimeUnit(internationalizationService),
            new LengthUnit(internationalizationService));

    private final Converter converter = new Converter(units, bot, speechService);

    @BeforeEach
    void init() {
        UnitUtils.addLengthUnitTranslations(internationalizationService);
        UnitUtils.addTimeUnitTranslations(internationalizationService);

        units.forEach(unit -> ReflectionTestUtils.invokeMethod(unit, "postConstruct"));
    }

    @Test
    void parseWithWrongInputTest() {
        BotRequest requestFromGroup = TestUtils.getRequestFromGroup("конверт 123");
        assertThrows(BotException.class, () -> converter.parse(requestFromGroup));
        verify(bot).sendTyping(requestFromGroup.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" 123 fs klmn", " 123 klmn fs"})
    void parseWithUnknownArgumentsTest(String arguments) {
        final String expectedResponseText = """
                <b>${command.converter.time.caption}</b>
                ${command.converter.time.femtosecond} — fs
                ${command.converter.time.picosecond} — ps
                ${command.converter.time.nanosecond} — ns
                ${command.converter.time.microsecond} — mks
                ${command.converter.time.millisecond} — ms
                ${command.converter.time.centisecond} — cs
                ${command.converter.time.second} — s
                ${command.converter.time.minute} — m
                ${command.converter.time.hour} — h
                ${command.converter.time.day} — d
                ${command.converter.time.year} — y
                ${command.converter.time.century} — c
                
                <b>${command.converter.length.caption}</b>
                ${command.converter.length.femtometer} — fm
                ${command.converter.length.picometer} — pm
                ${command.converter.length.nanometer} — nm
                ${command.converter.length.micrometer} — mk
                ${command.converter.length.millimeter} — mm
                ${command.converter.length.centimeter} — cm
                ${command.converter.length.inch} — inch
                ${command.converter.length.decimeter} — dm
                ${command.converter.length.foor} — ft
                ${command.converter.length.yard} — yd
                ${command.converter.length.meter} — m
                ${command.converter.length.kilometer} — km
                ${command.converter.length.mile} — mi
                ${command.converter.length.mn} — mn
                """;
        BotRequest request = TestUtils.getRequestFromGroup("convert" + arguments);

        BotResponse botResponse = converter.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @ParameterizedTest
    @MethodSource("provideValues")
    void parseTest(BigDecimal value, String from, String to, String expected) {
        BotRequest request = TestUtils.getRequestFromGroup("convert " + value + " " + from + " " + to);

        BotResponse botResponse = converter.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expected, textResponse.getText());
    }

    private static Stream<Arguments> provideValues() {
        return Stream.of(
                Arguments.of(BigDecimal.ONE, "s", "mks", "1 ${command.converter.time.second} = <b>1000000 ${command.converter.time.microsecond}</b>\n( * 1000000)"),
                Arguments.of(BigDecimal.ONE, "mks", "s", "1 ${command.converter.time.microsecond} = <b>0.000001 ${command.converter.time.second}</b>\n( / 1000000)"),
                Arguments.of(BigDecimal.ONE, "s", "s", "1 ${command.converter.time.second} = <b>1 ${command.converter.time.second}</b>\n( * 1)"),

                Arguments.of(BigDecimal.ONE, "km", "mm", "1 ${command.converter.length.kilometer} = <b>1000000 ${command.converter.length.millimeter}</b>\n( * 1000000)"),
                Arguments.of(BigDecimal.ONE, "mm", "km", "1 ${command.converter.length.millimeter} = <b>0.000001 ${command.converter.length.kilometer}</b>\n( / 1000000)"),
                Arguments.of(BigDecimal.ONE, "mm", "mm", "1 ${command.converter.length.millimeter} = <b>1 ${command.converter.length.millimeter}</b>\n( * 1)"),
                Arguments.of(BigDecimal.ONE, "mi", "m", "1 ${command.converter.length.mile} = <b>1609.34 ${command.converter.length.meter}</b>\n( * 1609.34)"),
                Arguments.of(BigDecimal.ONE, "yd", "m", "1 ${command.converter.length.yard} = <b>0.9141 ${command.converter.length.meter}</b>\n( / 1.094)"),
                Arguments.of(BigDecimal.ONE, "ft", "m", "1 ${command.converter.length.foor} = <b>0.3048 ${command.converter.length.meter}</b>\n( / 3.281)"),
                Arguments.of(BigDecimal.ONE, "mn", "m", "1 ${command.converter.length.mn} = <b>1852 ${command.converter.length.meter}</b>\n( * 1852)"),
                Arguments.of(BigDecimal.ONE, "inch", "cm", "1 ${command.converter.length.inch} = <b>2.54 ${command.converter.length.centimeter}</b>\n( * 2.54)"),
                Arguments.of(new BigDecimal("1000000000000000000"), "km", "fm", "1000000000000000000 ${command.converter.length.kilometer} = <b>1000000000000000000000000000000000000 ${command.converter.length.femtometer}</b>\n( * 1000000000000000000)")
        );
    }

}