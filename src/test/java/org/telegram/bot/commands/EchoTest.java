package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageKind;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.ObjectCopier;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.telegram.bot.TestUtils.*;

@ExtendWith(MockitoExtension.class)
class EchoTest {

    @Mock
    private SpeechService speechService;
    @Mock
    private TalkerWordService talkerWordService;
    @Mock
    private TalkerPhraseService talkerPhraseService;
    @Mock
    private CommandPropertiesService commandPropertiesService;
    @Mock
    private TalkerDegreeService talkerDegreeService;
    @Mock
    private Bot bot;
    @Mock
    private ObjectCopier objectCopier;

    @InjectMocks
    private Echo echo;

    @Test
    void parseWithEmptyText() {
        final String expectedResponseText = "чо?";
        BotRequest request = getRequestFromGroup("bot");

        when(speechService.getRandomMessageByTag(BotSpeechTag.ECHO)).thenReturn(expectedResponseText);

        BotResponse botResponse = echo.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        verify(bot).sendTyping(request.getMessage().getChatId());
        checkDefaultTextResponseParams(textResponse);

        verify(speechService).getRandomMessageByTag(BotSpeechTag.ECHO);
    }

    @Test
    void parseWithText() {
        final String expectedResponseText1 = "пока не родила";
        final String expectedResponseText2 = "нормально";
        Chat chat = new Chat().setChatId(DEFAULT_CHAT_ID);
        BotRequest request = getRequestFromGroup("как дела?");

        TalkerPhrase firstPhrase = new TalkerPhrase()
                .setId(1L)
                .setChat(chat)
                .setPhrase(expectedResponseText1);
        TalkerPhrase secondPhrase = new TalkerPhrase()
                .setId(2L)
                .setChat(chat)
                .setPhrase(expectedResponseText2);
        TalkerPhrase thirdPhrase = new TalkerPhrase()
                .setId(2L)
                .setChat(chat)
                .setPhrase(expectedResponseText2);
        java.util.Set<TalkerWord> talkerWords = new HashSet<>(
                List.of(
                        new TalkerWord()
                                .setId(1L)
                                .setWord("как")
                                .setPhrases(new HashSet<>(List.of(firstPhrase))),
                        new TalkerWord()
                                .setId(2L)
                                .setWord("дела")
                                .setPhrases(new HashSet<>(List.of(firstPhrase, secondPhrase))),
                        new TalkerWord()
                                .setId(2L)
                                .setWord("дела")
                                .setPhrases(new HashSet<>(List.of(thirdPhrase)))
                        ));

        when(talkerWordService.get(anyList(), anyLong())).thenReturn(talkerWords);

        BotResponse botResponse = echo.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        verify(bot).sendTyping(request.getMessage().getChatId());
        checkDefaultTextResponseParams(textResponse);

        String actualResponseText = textResponse.getText();
        assertTrue(expectedResponseText1.equals(actualResponseText) || expectedResponseText2.equals(actualResponseText));
    }

    @Test
    void parseWithTextAndOnePhrase() {
        final String expectedResponseText = "пока не родила";
        Chat chat = new Chat().setChatId(DEFAULT_CHAT_ID);
        BotRequest request = getRequestFromGroup("как дела?");

        TalkerPhrase firstPhrase = new TalkerPhrase()
                .setId(1L)
                .setChat(chat)
                .setPhrase(expectedResponseText);
        java.util.Set<TalkerWord> talkerWords = new HashSet<>(
                List.of(
                        new TalkerWord()
                                .setId(1L)
                                .setWord("как")
                                .setPhrases(new HashSet<>(List.of(firstPhrase)))));

        when(talkerWordService.get(anyList(), anyLong())).thenReturn(talkerWords);

        BotResponse botResponse = echo.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        verify(bot).sendTyping(request.getMessage().getChatId());
        checkDefaultTextResponseParams(textResponse);

        String actualResponseText = textResponse.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void analyzeCallbackQueryTest() {
        org.telegram.bot.domain.model.request.Message message = new Message()
                .setText("id")
                .setMessageKind(MessageKind.CALLBACK);

        assertDoesNotThrow(() -> echo.analyze(new BotRequest().setMessage(message)));
    }

    @Test
    void analyzeWithoutTextMessageTest() {
        BotRequest request = getRequestFromGroup(null);

        assertDoesNotThrow(() -> echo.analyze(request));
    }

    @Test
    void analyzeWithoutReplyToMessageTest() {
        BotRequest request = getRequestFromGroup();

        when(bot.getBotUsername()).thenReturn(BOT_USERNAME);
        when(talkerDegreeService.get(anyLong())).thenReturn(new TalkerDegree().setDegree(1));

        assertDoesNotThrow(() -> echo.analyze(request));
    }

    @Test
    void analyzeWithAppealToBot() {
        BotRequest request = getRequestFromGroup("@" + BOT_USERNAME + " как дела?");
        CommandProperties commandProperties = new CommandProperties().setCommandName("echo");

        when(bot.getBotUsername()).thenReturn(BOT_USERNAME);
        when(commandPropertiesService.getCommand(any(Class.class))).thenReturn(commandProperties);
        when(objectCopier.copyObject(request, BotRequest.class)).thenReturn(request);

        assertDoesNotThrow(() -> echo.analyze(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void analyzeWithReplyToMessage() {
        User user = new User()
                .setUsername(BOT_USERNAME);

        Message replyToMessage = new Message();
        replyToMessage.setUser(user);
        replyToMessage.setText("как дела?");

        BotRequest request = getRequestWithRepliedMessage(replyToMessage);

        CommandProperties commandProperties = new CommandProperties().setCommandName("echo");

        when(bot.getBotUsername()).thenReturn(BOT_USERNAME);
        when(commandPropertiesService.getCommand(any(Class.class))).thenReturn(commandProperties);
        when(objectCopier.copyObject(request, BotRequest.class)).thenReturn(request);

        assertDoesNotThrow(() -> echo.analyze(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(talkerPhraseService).save(anySet(), any(Chat.class));
    }

    @Test
    void analyzeWithTalkerDegreeWorks() {
        BotRequest request = getRequestFromGroup("как дела?");
        CommandProperties commandProperties = new CommandProperties().setCommandName("echo");
        TalkerDegree talkerDegree = new TalkerDegree().setChat(new Chat().setChatId(DEFAULT_CHAT_ID)).setDegree(100);

        when(bot.getBotUsername()).thenReturn(BOT_USERNAME);
        when(commandPropertiesService.getCommand(any(Class.class))).thenReturn(commandProperties);
        when(talkerDegreeService.get(anyLong())).thenReturn(talkerDegree);
        when(objectCopier.copyObject(request, BotRequest.class)).thenReturn(request);

        assertDoesNotThrow(() -> echo.analyze(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
    }
}