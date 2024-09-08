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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.telegram.bot.commands.Speller.SPELLER_API_URL;

@ExtendWith(MockitoExtension.class)
class SpellerTest {

    @Mock
    private Bot bot;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private SpeechService speechService;

    @Mock
    private ResponseEntity<Speller.SpellResult[]> response;

    @InjectMocks
    private Speller speller;

    @Test
    void parseWithoutArgumentsTest() {
        final String expectedResponseText = "${command.speller.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup("speller");

        BotResponse botResponse = speller.parse(request).get(0);

        verify(commandWaitingService).add(request.getMessage(), Speller.class);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithoutArgumentsWithRepliedMessageWithoutTextTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestWithRepliedMessage("");
        request.getMessage().getReplyToMessage().setText(null);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> speller.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithoutArgumentsWithRepliedMessageTest() {
        final String spellingText = "text";
        final String expectedResponseText = """
                <u>${command.speller.errorsfound}</u>
                <s>word1</s> — s1, s2
                <s>word2</s> — s3, s4
                """;
        BotRequest request = TestUtils.getRequestWithRepliedMessage(spellingText);

        when(response.getBody())
                .thenReturn(new Speller.SpellResult[]{
                        new Speller.SpellResult().setWord("word1").setS(List.of("s1, s2")),
                        new Speller.SpellResult().setWord("word2").setS(List.of("s3, s4"))
                });
        when(botRestTemplate.getForEntity(SPELLER_API_URL + spellingText, Speller.SpellResult[].class))
                .thenReturn(response);

        BotResponse botResponse = speller.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        assertEquals(request.getMessage().getReplyToMessage().getMessageId(), textResponse.getReplyToMessageId());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithArgumentsAndRestClientExceptionTest() {
        final String spellingText = "text";
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("speller " + spellingText);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(botRestTemplate.getForEntity(SPELLER_API_URL + spellingText, Speller.SpellResult[].class))
                .thenThrow(new RestClientException("no response"));
        when(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> speller.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithArgumentsAndNullResponseText() {
        final String spellingText = "text";
        final String expectedResponseText = "${command.speller.noerrorsfound}";
        BotRequest request = TestUtils.getRequestFromGroup("speller " + spellingText);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(response.getBody()).thenReturn(null);
        when(botRestTemplate.getForEntity(SPELLER_API_URL + spellingText, Speller.SpellResult[].class))
                .thenReturn(response);

        BotResponse botResponse = speller.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(request.getMessage().getMessageId(), textResponse.getReplyToMessageId());
        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithArgumentsAndEmptyResponseText() {
        final String spellingText = "text";
        final String expectedResponseText = "${command.speller.noerrorsfound}";
        BotRequest request = TestUtils.getRequestFromGroup("speller " + spellingText);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(response.getBody()).thenReturn(new Speller.SpellResult[]{});
        when(botRestTemplate.getForEntity(SPELLER_API_URL + spellingText, Speller.SpellResult[].class))
                .thenReturn(response);

        BotResponse botResponse = speller.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
        assertEquals(request.getMessage().getMessageId(), textResponse.getReplyToMessageId());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithArgumentsText() {
        final String spellingText = "text";
        final String expectedResponseText = """
                <u>${command.speller.errorsfound}</u>
                <s>word1</s> — s1, s2
                <s>word2</s> — s3, s4
                """;
        BotRequest request = TestUtils.getRequestFromGroup("speller " + spellingText);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(response.getBody())
                .thenReturn(new Speller.SpellResult[]{
                        new Speller.SpellResult().setWord("word1").setS(List.of("s1, s2")),
                        new Speller.SpellResult().setWord("word2").setS(List.of("s3, s4"))
                });
        when(botRestTemplate.getForEntity(SPELLER_API_URL + spellingText, Speller.SpellResult[].class))
                .thenReturn(response);

        BotResponse botResponse = speller.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
        assertEquals(request.getMessage().getMessageId(), textResponse.getReplyToMessageId());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

}