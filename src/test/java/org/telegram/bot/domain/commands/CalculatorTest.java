package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculatorTest {
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private RestTemplate defaultRestTemplate;
    @Mock
    private ResponseEntity<Object> response;

    @InjectMocks
    private Calculator calculator;

    @Test
    void parseWithEmptyTextTest() {
        final String expectedText = "теперь напиши мне что нужно посчитать";
        Update update = TestUtils.getUpdate();

        SendMessage sendMessage = calculator.parse(update);
        assertNotNull(sendMessage);

        String actualText = sendMessage.getText();
        assertEquals(expectedText, actualText);

        verify(commandWaitingService).add(update.getMessage(), Calculator.class);
    }

    @Test
    void parseWithNoResponseTest() {
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(response);

        assertThrows(BotException.class, () -> calculator.parse(TestUtils.getUpdate("calc test")));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithRequestErrorTest() {
        final String expectedErrorText = "Undefined symbol test";

        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenThrow(
                        new HttpClientErrorException(
                                HttpStatus.BAD_REQUEST,
                                "",
                                ("{\"error\":\"" + expectedErrorText + "\"}").getBytes(StandardCharsets.UTF_8),
                                StandardCharsets.UTF_8));

        SendMessage sendMessage = calculator.parse(TestUtils.getUpdate("calc test"));
        assertNotNull(sendMessage);

        String actualErrorText = sendMessage.getText();
        assertEquals(expectedErrorText, actualErrorText);
    }

    @ParameterizedTest
    @ValueSource(strings = {"6", "Infinite"})
    void parseTest(String expressionResult) {
        final String expectedResponseText = "`" + expressionResult + "`";

        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenReturn(response);
        when(response.getBody()).thenReturn("{\"result\":\"" + expressionResult + "\"}");

        SendMessage sendMessage = calculator.parse(TestUtils.getUpdate("calc test"));
        assertNotNull(sendMessage);

        String actualResponseText = sendMessage.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

}