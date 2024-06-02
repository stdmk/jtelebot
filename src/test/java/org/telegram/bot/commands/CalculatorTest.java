package org.telegram.bot.commands;

import org.json.JSONException;
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
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.telegram.bot.TestUtils.checkDefaultTextResponseParams;
import static org.telegram.bot.TestUtils.getRequestFromGroup;

@ExtendWith(MockitoExtension.class)
class CalculatorTest {

    @Mock
    private Bot bot;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private RestTemplate defaultRestTemplate;
    @Mock
    private BotStats botStats;
    @Mock
    private ResponseEntity<Object> response;

    @InjectMocks
    private Calculator calculator;

    @Test
    void parseWithEmptyTextTest() {
        final String expectedText = "${command.calculator.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup();

        BotResponse botResponse = calculator.parse(request).get(0);
        TextResponse textResponse = checkDefaultTextResponseParams(botResponse);

        assertNotNull(textResponse);

        String actualText = textResponse.getText();
        assertEquals(expectedText, actualText);

        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(commandWaitingService).add(request.getMessage(), Calculator.class);
    }

    @Test
    void parseWithNoResponseTest() {
        BotRequest request = getRequestFromGroup("calc test");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(response);

        assertThrows(BotException.class, () -> calculator.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithRequestErrorTest() {
        final String expectedErrorText = "Undefined symbol test";
        BotRequest request = getRequestFromGroup("calc test");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenThrow(
                        new HttpClientErrorException(
                                HttpStatus.BAD_REQUEST,
                                "",
                                ("{\"error\":\"" + expectedErrorText + "\"}").getBytes(StandardCharsets.UTF_8),
                                StandardCharsets.UTF_8));

        BotResponse botResponse = calculator.parse(request).get(0);
        TextResponse textResponse = checkDefaultTextResponseParams(botResponse);

        verify(bot).sendTyping(request.getMessage().getChatId());
        checkDefaultTextResponseParams(textResponse, FormattingStyle.MARKDOWN);

        String actualErrorText = textResponse.getText();
        assertEquals(expectedErrorText, actualErrorText);
    }

    @Test
    void parseWithJsonExceptionTest() {
        BotRequest request = getRequestFromGroup("calc test");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenReturn(response);
        when(response.getBody()).thenReturn("{{{");

        assertThrows(BotException.class, () -> calculator.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(botStats).incrementErrors(any(BotRequest.class), any(JSONException.class), anyString());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"6", "Infinite"})
    void parseTest(String expressionResult) {
        final String expectedResponseText = "`" + expressionResult + "`";
        BotRequest request = getRequestFromGroup("calc test");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenReturn(response);
        when(response.getBody()).thenReturn("{\"result\":\"" + expressionResult + "\"}");

        BotResponse botResponse = calculator.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());
        checkDefaultTextResponseParams(botResponse, FormattingStyle.MARKDOWN);
        TextResponse textResponse = checkDefaultTextResponseParams(botResponse);

        String actualResponseText = textResponse.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

}