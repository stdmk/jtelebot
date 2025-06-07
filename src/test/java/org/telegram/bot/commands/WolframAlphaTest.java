package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WolframAlphaTest {

    @Mock
    private Bot bot;
    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private SpeechService speechService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private BotStats botStats;

    @Mock
    private ResponseEntity<WolframAlpha.WolframAlphaData> response;

    @InjectMocks
    private WolframAlpha wolframAlpha;

    @Test
    void parseWithoutTokenTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("wolframalpha");

        when(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> wolframAlpha.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());
        verify(bot, never()).sendUploadPhoto(request.getMessage().getChatId());
    }

    @Test
    void parseWithoutArgumentTest() {
        final String expectedResponseText = "${command.wolframalpfa.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup("wolframalpha");
        Message message = request.getMessage();

        when(propertiesConfig.getWolframAlphaToken()).thenReturn("token");
        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());

        BotResponse botResponse = wolframAlpha.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(commandWaitingService).add(message, WolframAlpha.class);
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseButNoResponseTest() {
        final String expectedErrorText = "error";
        final String token = "token";
        final String argument = "test";
        final String expectedApiUrl = "http://api.wolframalpha.com/v2/query?output=json&includepodid=Result&appid=" + token + "&input=" + argument;
        BotRequest request = TestUtils.getRequestFromGroup("wolframalpha " + argument);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(propertiesConfig.getWolframAlphaToken()).thenReturn(token);
        when(botRestTemplate.getForEntity(expectedApiUrl, WolframAlpha.WolframAlphaData.class)).thenThrow(new RestClientException(""));
        when(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> wolframAlpha.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    @ParameterizedTest
    @MethodSource("provideWolframAlphaData")
    void parseButNothingFoundTest(WolframAlpha.WolframAlphaData wolframAlphaData) {
        final String expectedErrorText = "error";
        final String token = "token";
        final String argument = "test";
        final String expectedApiUrl = "http://api.wolframalpha.com/v2/query?output=json&includepodid=Result&appid=" + token + "&input=" + argument;
        BotRequest request = TestUtils.getRequestFromGroup("wolframalpha " + argument);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(propertiesConfig.getWolframAlphaToken()).thenReturn(token);
        when(response.getBody()).thenReturn(wolframAlphaData);
        when(botRestTemplate.getForEntity(expectedApiUrl, WolframAlpha.WolframAlphaData.class)).thenReturn(response);
        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorText);

        BotResponse botResponse = wolframAlpha.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedErrorText, textResponse.getText());

        verify(botStats).incrementWorlframRequests();
        verify(bot).sendTyping(message.getChatId());
    }

    private static Stream<WolframAlpha.WolframAlphaData> provideWolframAlphaData() {
        return Stream.of(
                null,
                new WolframAlpha.WolframAlphaData(),
                new WolframAlpha.WolframAlphaData().setQueryresult(new WolframAlpha.QueryResult()),
                new WolframAlpha.WolframAlphaData().setQueryresult(new WolframAlpha.QueryResult().setPods(List.of())),
                new WolframAlpha.WolframAlphaData().setQueryresult(new WolframAlpha.QueryResult().setPods(List.of(new WolframAlpha.Pod()))),
                new WolframAlpha.WolframAlphaData().setQueryresult(new WolframAlpha.QueryResult().setPods(List.of(new WolframAlpha.Pod().setSubpods(List.of())))),
                new WolframAlpha.WolframAlphaData().setQueryresult(new WolframAlpha.QueryResult().setPods(List.of(new WolframAlpha.Pod().setSubpods(List.of(new WolframAlpha.SubPod()))))),
                new WolframAlpha.WolframAlphaData().setQueryresult(new WolframAlpha.QueryResult().setPods(List.of(new WolframAlpha.Pod().setSubpods(List.of(new WolframAlpha.SubPod().setPlaintext(""))))))
        );
    }

    @Test
    void parseTest() {
        final String expectedResponseText = "response";
        final String token = "token";
        final String argument = "test";
        final String expectedApiUrl = "http://api.wolframalpha.com/v2/query?output=json&includepodid=Result&appid=" + token + "&input=" + argument;
        BotRequest request = TestUtils.getRequestFromGroup("wolframalpha " + argument);
        Message message = request.getMessage();

        WolframAlpha.WolframAlphaData wolframAlphaData = new WolframAlpha.WolframAlphaData()
                .setQueryresult(new WolframAlpha.QueryResult()
                        .setPods(List.of(new WolframAlpha.Pod()
                                .setSubpods(List.of(new WolframAlpha.SubPod()
                                        .setPlaintext(expectedResponseText))))));

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(propertiesConfig.getWolframAlphaToken()).thenReturn(token);
        when(response.getBody()).thenReturn(wolframAlphaData);
        when(botRestTemplate.getForEntity(expectedApiUrl, WolframAlpha.WolframAlphaData.class)).thenReturn(response);

        BotResponse botResponse = wolframAlpha.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(botStats).incrementWorlframRequests();
        verify(bot).sendTyping(message.getChatId());
    }

}