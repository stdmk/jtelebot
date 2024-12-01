package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.ObjectCopier;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnTest {

    private static final String RU_LAYOUT = "1234567890-=йцукенгшщзхъфывапролджэячсмитьбю.\\\\!\"№;%:?*()_+ЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ,/";

    @Mock
    private Bot bot;
    @Mock
    private ObjectCopier objectCopier;
    @Mock
    private SpeechService speechService;
    @Mock
    private CommandPropertiesService commandPropertiesService;
    @Mock
    private LanguageResolver languageResolver;
    @Mock
    private InternationalizationService internationalizationService;

    @InjectMocks
    private Turn turn;

    @Test
    void parseWithoutArgumentsAndReplyMessageTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> turn.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithArgumentsAndReplyMessageTest() {
        final String expectedResponseText = "привет";
        final String lang = "ru";
        BotRequest request = TestUtils.getRequestWithRepliedMessage("ghbdtn vbh");
        request.getMessage().setText("turn ghbdtn");

        when(languageResolver.getChatLanguageCode(request)).thenReturn(lang);
        when(internationalizationService.internationalize("${command.turn.layout}", lang)).thenReturn(RU_LAYOUT);

        BotResponse botResponse = turn.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithoutArgumentsAndWithReplyMessageTest() {
        final String expectedResponseText = "hello world";
        final String lang = "ru";
        BotRequest request = TestUtils.getRequestWithRepliedMessage("руддщ цщкдв");
        request.getMessage().setText("turn");

        when(languageResolver.getChatLanguageCode(request)).thenReturn(lang);
        when(internationalizationService.internationalize("${command.turn.layout}", lang)).thenReturn(RU_LAYOUT);

        BotResponse botResponse = turn.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void analyzeWithoutArgumentsTest() {
        BotRequest request = TestUtils.getRequestFromGroup(null);

        List<BotResponse> botResponses = turn.analyze(request);

        assertTrue(botResponses.isEmpty());
    }

    @Test
    void analyzeWithEmptyTextAsArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("");

        List<BotResponse> botResponses = turn.analyze(request);

        assertTrue(botResponses.isEmpty());
    }

    @Test
    void analyzeWithTurnCommandAsArgumentsTest() {
        BotRequest request = TestUtils.getRequestFromGroup("turn ghbdtn");

        List<BotResponse> botResponses = turn.analyze(request);

        assertTrue(botResponses.isEmpty());
    }

    @Test
    void analyzeWithNotNeededToTurnTest() {
        final String lang = "ru";
        BotRequest request = TestUtils.getRequestFromGroup("привет мир");

        when(languageResolver.getChatLanguageCode(request)).thenReturn(lang);
        when(internationalizationService.internationalize("${command.turn.pattern}")).thenReturn(Set.of("[а-яА-Я]+"));
        ReflectionTestUtils.invokeMethod(turn, "postConstruct");

        List<BotResponse> botResponses = turn.analyze(request);

        assertTrue(botResponses.isEmpty());
    }

    @Test
    void analyzeWithNotNeededToTurnTest2() {
        final String lang = "en";
        BotRequest request = TestUtils.getRequestFromGroup("привет мир");

        when(languageResolver.getChatLanguageCode(request)).thenReturn(lang);
        when(internationalizationService.internationalize("${command.turn.pattern}")).thenReturn(Set.of("[а-яА-Я]+"));
        ReflectionTestUtils.invokeMethod(turn, "postConstruct");

        List<BotResponse> botResponses = turn.analyze(request);

        assertTrue(botResponses.isEmpty());
    }

    @Test
    void analyzeWithNotFindNeededToTurnTextTest() {
        final String lang = "ru";
        BotRequest request = TestUtils.getRequestFromGroup("hello world");

        when(languageResolver.getChatLanguageCode(request)).thenReturn(lang);
        when(internationalizationService.internationalize("${command.turn.pattern}")).thenReturn(Set.of("[а-яА-Я]+"));
        ReflectionTestUtils.invokeMethod(turn, "postConstruct");

        List<BotResponse> botResponses = turn.analyze(request);

        assertTrue(botResponses.isEmpty());
    }

    @Test
    void analyzeWithErrorInObjectCopierTest() {
        final String lang = "ru";
        BotRequest request = TestUtils.getRequestFromGroup("ghbdtn vbh");

        when(languageResolver.getChatLanguageCode(request)).thenReturn(lang);
        when(internationalizationService.internationalize("${command.turn.pattern}")).thenReturn(Set.of("[а-яА-Я]+"));
        when(commandPropertiesService.getCommand(Turn.class)).thenReturn(new CommandProperties().setCommandName("turn"));
        ReflectionTestUtils.invokeMethod(turn, "postConstruct");

        List<BotResponse> botResponses = turn.analyze(request);

        assertTrue(botResponses.isEmpty());
    }

    @Test
    void analyzeTest() {
        final String expectedResponseText = "привет мир";
        final String lang = "ru";
        BotRequest request = TestUtils.getRequestFromGroup("ghbdtn vbh");

        when(languageResolver.getChatLanguageCode(request)).thenReturn(lang);
        when(internationalizationService.internationalize("${command.turn.pattern}")).thenReturn(Set.of("[а-яА-Я]+"));
        when(commandPropertiesService.getCommand(Turn.class)).thenReturn(new CommandProperties().setCommandName("turn"));
        when(objectCopier.copyObject(request, BotRequest.class)).thenReturn(request);
        when(languageResolver.getChatLanguageCode(request)).thenReturn(lang);
        when(internationalizationService.internationalize("${command.turn.layout}", lang)).thenReturn(RU_LAYOUT);
        ReflectionTestUtils.invokeMethod(turn, "postConstruct");

        BotResponse botResponse = turn.analyze(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

}