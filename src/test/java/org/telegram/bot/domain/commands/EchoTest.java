package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.commands.Echo;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

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

    @InjectMocks
    private Echo echo;

    @Test
    void parseWithEmptyText() {
        final String expectedResponseText = "чо?";
        Update update = getUpdateFromGroup("bot");

        when(speechService.getRandomMessageByTag(BotSpeechTag.ECHO)).thenReturn(expectedResponseText);

        SendMessage sendMessage = echo.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        checkDefaultSendMessageParams(sendMessage);

        verify(speechService).getRandomMessageByTag(BotSpeechTag.ECHO);
    }

    @Test
    void parseWithText() {
        final String expectedResponseText1 = "пока не родила";
        final String expectedResponseText2 = "нормально";
        Chat chat = new Chat().setChatId(DEFAULT_CHAT_ID);
        Update update = getUpdateFromGroup("как дела?");

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

        SendMessage sendMessage = echo.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        checkDefaultSendMessageParams(sendMessage);

        String actualResponseText = sendMessage.getText();
        assertTrue(expectedResponseText1.equals(actualResponseText) || expectedResponseText2.equals(actualResponseText));
    }

    @Test
    void parseWithTextAndOnePhrase() {
        final String expectedResponseText = "пока не родила";
        Chat chat = new Chat().setChatId(DEFAULT_CHAT_ID);
        Update update = getUpdateFromGroup("как дела?");

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

        SendMessage sendMessage = echo.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        checkDefaultSendMessageParams(sendMessage);

        String actualResponseText = sendMessage.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void analyzeCallbackQueryTest() {
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("id");
        Update update = new Update();
        update.setCallbackQuery(callbackQuery);

        assertDoesNotThrow(() -> echo.analyze(echo, update));
    }

    @Test
    void analyzeWithoutTextMessageTest() {
        Update update = getUpdateFromGroup(null);

        assertDoesNotThrow(() -> echo.analyze(echo, update));
    }

    @Test
    void analyzeWithoutReplyToMessageTest() {
        Update update = getUpdateFromGroup();

        when(bot.getBotUsername()).thenReturn(BOT_USERNAME);
        when(talkerDegreeService.get(anyLong())).thenReturn(new TalkerDegree().setDegree(1));

        assertDoesNotThrow(() -> echo.analyze(echo, update));
    }

    @Test
    void analyzeWithAppealToBot() {
        Update update = getUpdateFromGroup("@" + BOT_USERNAME + " как дела?");
        CommandProperties commandProperties = new CommandProperties().setCommandName("echo");

        when(bot.getBotUsername()).thenReturn(BOT_USERNAME);
        when(commandPropertiesService.getCommand(any(Class.class))).thenReturn(commandProperties);

        assertDoesNotThrow(() -> echo.analyze(echo, update));
        verify(bot).sendTyping(update.getMessage().getChatId());
    }

    @Test
    void analyzeWithReplyToMessage() {
        org.telegram.telegrambots.meta.api.objects.User user = new org.telegram.telegrambots.meta.api.objects.User();
        user.setUserName(BOT_USERNAME);

        Message replyToMessage = new Message();
        replyToMessage.setFrom(user);
        replyToMessage.setText("как дела?");

        Update update = getUpdateWithRepliedMessage(replyToMessage);

        CommandProperties commandProperties = new CommandProperties().setCommandName("echo");

        when(bot.getBotUsername()).thenReturn(BOT_USERNAME);
        when(commandPropertiesService.getCommand(any(Class.class))).thenReturn(commandProperties);

        assertDoesNotThrow(() -> echo.analyze(echo, update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(talkerPhraseService).save(anySet(), any(Chat.class));
    }

    @Test
    void analyzeWithTalkerDegreeWorks() {
        Update update = getUpdateFromGroup("как дела?");
        CommandProperties commandProperties = new CommandProperties().setCommandName("echo");
        TalkerDegree talkerDegree = new TalkerDegree().setChat(new Chat().setChatId(DEFAULT_CHAT_ID)).setDegree(100);

        when(bot.getBotUsername()).thenReturn(BOT_USERNAME);
        when(commandPropertiesService.getCommand(any(Class.class))).thenReturn(commandProperties);
        when(talkerDegreeService.get(anyLong())).thenReturn(talkerDegree);

        assertDoesNotThrow(() -> echo.analyze(echo, update));
        verify(bot).sendTyping(update.getMessage().getChatId());
    }
}