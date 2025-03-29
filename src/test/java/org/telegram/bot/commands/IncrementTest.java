package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.IncrementService;
import org.telegram.bot.services.SpeechService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncrementTest {

    @Mock
    private SpeechService speechService;
    @Mock
    private IncrementService incrementService;

    @InjectMocks
    private Increment increment;

    @Test
    void parseWithoutArgumentsTest() {
        final String expectedResponseText = """
                <b><u>${command.increment.incrementsinfo}</u></b>:
                <b>name1:</b> 0
                <b>name2:</b> 1
                <b>name3:</b> 10""";
        BotRequest request = TestUtils.getRequestFromGroup("increment");
        Message message = request.getMessage();

        when(incrementService.get(message.getChat(), message.getUser())).thenReturn(getSomeIncrements());

        BotResponse botResponse = increment.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithUnknownIncrementNameAsArgumentTest() {
        final String expectedErrorText = "error";
        final String incrementName = "test";
        BotRequest request = TestUtils.getRequestFromGroup("increment " + incrementName);

        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> increment.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithIncrementNameAsArgumentTest() {
        final String expectedResponseText = "<b>test:</b> 0";
        final String incrementName = "test";
        BotRequest request = TestUtils.getRequestFromGroup("increment " + incrementName);
        Message message = request.getMessage();

        when(incrementService.get(message.getChat(), message.getUser(), incrementName)).thenReturn(getSomeIncrement(incrementName, BigDecimal.ZERO));

        BotResponse botResponse = increment.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "tratatam-tratatam"})
    void parseWithWrongIncrementValueAsArgumentTest(String value) {
        final String expectedErrorText = "error";
        final String incrementName = "test";
        BotRequest request = TestUtils.getRequestFromGroup("increment " + incrementName + " " + value);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> increment.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
        verify(incrementService, never()).save(any(org.telegram.bot.domain.entities.Increment.class));
    }

    @Test
    void parseWithUnknownIncrementNameAndValueAsArgumentTest() {
        final String expectedResponseText = "${command.increment.new} <b>test</b>: ${command.increment.withvalue} <b>1</b>";
        final String incrementName = "test";
        final BigDecimal incrementValue = BigDecimal.ONE;
        BotRequest request = TestUtils.getRequestFromGroup("increment " + incrementName + " " + incrementValue.toPlainString());
        Message message = request.getMessage();

        BotResponse botResponse = increment.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        ArgumentCaptor<org.telegram.bot.domain.entities.Increment> incrementCaptor = ArgumentCaptor.forClass(org.telegram.bot.domain.entities.Increment.class);
        verify(incrementService).save(incrementCaptor.capture());

        org.telegram.bot.domain.entities.Increment entity = incrementCaptor.getValue();
        assertEquals(message.getUser(), entity.getUser());
        assertEquals(message.getChat(), entity.getChat());
        assertEquals(incrementName, entity.getName());
        assertEquals(incrementValue, entity.getCount());
    }

    @Test
    void parseWithKnownIncrementNameAndZeroValueAsArgumentTest() {
        final String expectedResponseText = "${command.increment.deleted}: <b>test</b>";
        final String incrementName = "test";
        final BigDecimal incrementValue = BigDecimal.ZERO;
        BotRequest request = TestUtils.getRequestFromGroup("increment " + incrementName + " " + incrementValue.toPlainString());
        Message message = request.getMessage();

        org.telegram.bot.domain.entities.Increment entity = getSomeIncrement(incrementName, BigDecimal.ZERO);
        when(incrementService.get(message.getChat(), message.getUser(), incrementName)).thenReturn(entity);

        BotResponse botResponse = increment.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(incrementService).remove(entity);
    }

    @ParameterizedTest
    @MethodSource("provideValues")
    void parseWithIncrementNameAndValueAsArgumentTest(BigDecimal incrementValue, String expectedResponseText) {
        final String incrementName = "test";
        BotRequest request = TestUtils.getRequestFromGroup("increment " + incrementName + " " + incrementValue.toPlainString());
        Message message = request.getMessage();

        org.telegram.bot.domain.entities.Increment entity = getSomeIncrement(incrementName, BigDecimal.ZERO);
        when(incrementService.get(message.getChat(), message.getUser(), incrementName)).thenReturn(entity);

        BotResponse botResponse = increment.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(incrementService).save(entity);
    }

    private static Stream<Arguments> provideValues() {
        return Stream.of(
                Arguments.of(BigDecimal.valueOf(-1), "${command.increment.increment} <b>test</b> ${command.increment.reduced} ${command.increment.to} <b>-1</b>"),
                Arguments.of(BigDecimal.valueOf(1), "${command.increment.increment} <b>test</b> ${command.increment.increased} ${command.increment.to} <b>1</b>")
        );
    }

    private List<org.telegram.bot.domain.entities.Increment> getSomeIncrements() {
        return List.of(
                getSomeIncrement("name1", BigDecimal.ZERO),
                getSomeIncrement("name2", BigDecimal.ONE),
                getSomeIncrement("name3", BigDecimal.TEN)
        );
    }

    private org.telegram.bot.domain.entities.Increment getSomeIncrement(String name, BigDecimal value) {
        return new org.telegram.bot.domain.entities.Increment().setName(name).setCount(value);
    }

}