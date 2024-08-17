package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhoneNumberTest {

    private static final String API_URL = "http://rosreestr.subnets.ru/?format=json&get=num&num=";

    @Mock
    private ResponseEntity<PhoneNumber.ApiResponse> response;

    @Mock
    private Bot bot;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private RestTemplate botRestTemplate;

    @InjectMocks
    private PhoneNumber phoneNumber;

    @Test
    void parseWithoutArgumentTest() {
        final String expectedResponseText = "${command.phonenumber.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup("number ");

        BotResponse botResponse = phoneNumber.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(commandWaitingService).add(request.getMessage(), PhoneNumber.class);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithRestClientExceptionTest() {
        final String number = "123";
        BotRequest request = TestUtils.getRequestFromGroup("number " + number);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(botRestTemplate.getForEntity(API_URL + number, PhoneNumber.ApiResponse.class))
                .thenThrow(new RestClientException("error"));

        assertThrows((BotException.class), () -> phoneNumber.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithRestClientExceptionJsonIncludesTest() {
        final String errorText = "No data";
        final String number = "123";
        BotRequest request = TestUtils.getRequestFromGroup("number " + number);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(botRestTemplate.getForEntity(API_URL + number, PhoneNumber.ApiResponse.class))
                .thenThrow(new RestClientException("404 Not Found: [{\"error\":\"" + errorText + "\"}]"));

        BotResponse botResponse = phoneNumber.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(errorText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithNullableApiResponseTest() {
        final String number = "123";
        BotRequest request = TestUtils.getRequestFromGroup("number " + number);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(botRestTemplate.getForEntity(API_URL + number, PhoneNumber.ApiResponse.class))
                .thenReturn(response);

        assertThrows((BotException.class), () -> phoneNumber.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithResponseWithErrorTest() {
        final String number = "123";
        final String errorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("number " + number);
        Message message = request.getMessage();
        PhoneNumber.ApiResponse apiResponse = new PhoneNumber.ApiResponse().setError(errorText).setPhoneInfo(new PhoneNumber.PhoneInfo());

        when(response.getBody()).thenReturn(apiResponse);
        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(botRestTemplate.getForEntity(API_URL + number, PhoneNumber.ApiResponse.class))
                .thenReturn(response);

        BotResponse botResponse = phoneNumber.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(errorText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseTest() {
        final String number = "123";
        final String region = "region";
        final String operator = "operator";
        final String expectedResponseText = "<b>${command.phonenumber.operator}:</b> " + operator + "\n" +
                "<b>${command.phonenumber.region}:</b> " + region;
        BotRequest request = TestUtils.getRequestFromGroup("number " + number);
        Message message = request.getMessage();
        PhoneNumber.ApiResponse apiResponse = new PhoneNumber.ApiResponse()
                .setPhoneInfo(new PhoneNumber.PhoneInfo().setOperator(operator).setRegion(region));

        when(response.getBody()).thenReturn(apiResponse);
        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(botRestTemplate.getForEntity(API_URL + number, PhoneNumber.ApiResponse.class))
                .thenReturn(response);

        BotResponse botResponse = phoneNumber.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

}