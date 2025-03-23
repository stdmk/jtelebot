package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.model.CustomReactionsStats;
import org.telegram.bot.domain.model.ReactionsStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.bot.domain.model.request.MessageKind;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionsTest {

    private static final User USER = TestUtils.getUser();
    private static final User ANOTHER_USER = TestUtils.getUser(TestUtils.ANOTHER_USER_ID);

    @Mock
    private InternationalizationService internationalizationService;
    @Mock
    private UserService userService;
    @Mock
    private MessageService messageService;
    @Mock
    private MessageStatsService messageStatsService;
    @Mock
    private ReactionsStatsService reactionsStatsService;
    @Mock
    private CustomReactionsStatsService customReactionsStatsService;
    @Mock
    private ReactionDayStatsService reactionDayStatsService;
    @Mock
    private CustomReactionDayStatsService customReactionDayStatsService;
    @Mock
    private SpeechService speechService;
    @Mock
    private Bot bot;

    @InjectMocks
    private Reactions reactions;

    @Test
    void parseUnknownArgumentTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("today test");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> reactions.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parsePrivateMessageTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromPrivate("today");

        when(speechService.getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> reactions.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithoutTopMessageTest() {
        final String expectedResponseText = "${command.reactions.boringday}";
        BotRequest request = TestUtils.getRequestFromGroup("today");
        Message message = request.getMessage();
        Chat chat = message.getChat();

        when(messageStatsService.getByRepliesCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of());
        when(messageStatsService.getByReactionsCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of());

        BotResponse botResponse = reactions.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(chat.getChatId());
    }

    @Test
    void parseOnlyRepliesTest() {
        final String expectedResponseText = """
                <b>${command.reactions.caption}:</b>
                <u>${command.reactions.byreplies}:</u>
                1) <a href="https://t.me/c/1272607487/1">message1Text</a> (5)


                """;
        BotRequest request = TestUtils.getRequestFromGroup("today");
        Message message = request.getMessage();
        Chat chat = message.getChat();
        chat.setChatId(-1001272607487L);
        MessageStats messageStatsReplies = new MessageStats()
                .setId(1L)
                .setReplies(5)
                .setReactions(0)
                .setMessage(new org.telegram.bot.domain.entities.Message().setMessageId(1).setText("message1Text"));

        when(messageStatsService.getByRepliesCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of(messageStatsReplies));
        when(messageStatsService.getByReactionsCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of());
        when(reactionDayStatsService.get(chat)).thenReturn(List.of());
        when(customReactionDayStatsService.get(chat)).thenReturn(List.of());

        BotResponse botResponse = reactions.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(chat.getChatId());
    }

    @Test
    void parseOnlyReactionTest() {
        final String expectedResponseText = """
                <b>${command.reactions.caption}:</b>
                
                <u>${command.reactions.byreactions}:</u>
                1) <a href="https://t.me/c/1272607487/2">message2Text</a> (3)
                
                """;
        BotRequest request = TestUtils.getRequestFromGroup("today");
        Message message = request.getMessage();
        Chat chat = message.getChat();
        chat.setChatId(-1001272607487L);
        MessageStats messageStatsReactions = new MessageStats()
                .setId(2L)
                .setReplies(0)
                .setReactions(3)
                .setMessage(new org.telegram.bot.domain.entities.Message().setMessageId(2).setText("message2Text"));

        when(messageStatsService.getByRepliesCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of());
        when(messageStatsService.getByReactionsCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of(messageStatsReactions));
        when(reactionDayStatsService.get(chat)).thenReturn(List.of());
        when(customReactionDayStatsService.get(chat)).thenReturn(List.of());

        BotResponse botResponse = reactions.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(chat.getChatId());
    }

    @Test
    void parseWithoutArgumentsTest() {
        final String expectedResponseText = """
                <b>${command.reactions.caption}:</b>
                <u>${command.reactions.byreplies}:</u>
                1) <a href="https://t.me/c/1272607487/1">message1Text</a> (5)
                2) <a href="https://t.me/c/1272607487/3">${enum.messagecontenttype.photo}</a> (4)
                
                <u>${command.reactions.byreactions}:</u>
                1) <a href="https://t.me/c/1272607487/2">message2Text</a> (3)
                
                <u>${command.reactions.caption.byday}:</u>
                <code>1) 60 <a href="tg://user?id=1">username</a></code>
                emoji10emoji8emoji6emoji4emoji2emoji0
                <code>2) 50 <a href="tg://user?id=2">username</a></code>
                emoji9emoji7emoji5emoji3emoji1
                """;
        BotRequest request = TestUtils.getRequestFromGroup("today");
        Message message = request.getMessage();
        Chat chat = message.getChat();
        chat.setChatId(-1001272607487L);
        MessageStats messageStatsReplies1 = new MessageStats()
                .setId(1L)
                .setReplies(5)
                .setReactions(0)
                .setMessage(new org.telegram.bot.domain.entities.Message().setMessageId(1).setText("message1Text"));
        MessageStats messageStatsReplies2 = new MessageStats()
                .setId(3L)
                .setReplies(4)
                .setReactions(0)
                .setMessage(new org.telegram.bot.domain.entities.Message().setMessageId(3).setMessageContentType(MessageContentType.PHOTO));
        MessageStats messageStatsReactions = new MessageStats()
                .setId(2L)
                .setReplies(0)
                .setReactions(3)
                .setMessage(new org.telegram.bot.domain.entities.Message().setMessageId(2).setText("message2Text"));

        when(messageStatsService.getByRepliesCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of(messageStatsReplies1, messageStatsReplies2));
        when(messageStatsService.getByReactionsCountTop(eq(chat), any(LocalDate.class))).thenReturn(List.of(messageStatsReactions));
        ReactionsStats reactionsStats = getSomeReactionsStats();
        CustomReactionsStats customReactionsStats = getSomeCustomReactionsStats();
        when(reactionDayStatsService.get(chat)).thenReturn(reactionsStats.getReactionDayStatsList());
        when(customReactionDayStatsService.get(chat)).thenReturn(customReactionsStats.getCustomReactionDayStats());

        BotResponse botResponse = reactions.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(chat.getChatId());
    }

    @Test
    void parseTopAsArgumentTest() {
        final String expectedResponse = """
                <u>${command.reactions.caption.byday}:</u>
                <code>1) 60 <a href="tg://user?id=1">username</a></code>
                emoji10emoji8emoji6emoji4emoji2emoji0
                <code>2) 50 <a href="tg://user?id=2">username</a></code>
                emoji9emoji7emoji5emoji3emoji1
                
                <u>${command.reactions.caption.bymonth}:</u>
                <code>1) 160 <a href="tg://user?id=1">username</a></code>
                emoji20emoji18emoji16emoji14emoji12
                <code>2) 150 <a href="tg://user?id=2">username</a></code>
                emoji19emoji17emoji15emoji13emoji11
                
                <u>${command.reactions.caption.byall}:</u>
                <code>1) 300 <a href="tg://user?id=1">username</a></code>
                emoji30emoji28emoji26emoji24emoji22emoji20
                <code>2) 250 <a href="tg://user?id=2">username</a></code>
                emoji29emoji27emoji25emoji23emoji21

                """;
        final String topArgument = "top";
        BotRequest request = TestUtils.getRequestFromGroup("today " + topArgument);
        Chat chat = request.getMessage().getChat();

        when(internationalizationService.getAllTranslations("command.reactions.argumenttop")).thenReturn(Set.of(topArgument));
        when(reactionsStatsService.get(chat)).thenReturn(getSomeReactionsStats());
        when(customReactionsStatsService.get(chat)).thenReturn(getSomeCustomReactionsStats());

        ReflectionTestUtils.invokeMethod(reactions, "postConstruct");

        BotResponse response = reactions.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponse, textResponse.getText());
    }

    @Test
    void parseUserAsArgumentTest() {
        final String expectedResponse = """
                <b><a href="tg://user?id=1">username</a>:</b>
                <u>${command.reactions.caption.byday}:</u> 110(55 ${command.reactions.customreaction})
                emoji10emoji9emoji8emoji7emoji6emoji5emoji4emoji3emoji2emoji1
                <u>${command.reactions.caption.bymonth}:</u> 310(155 ${command.reactions.customreaction})
                emoji20emoji19emoji18emoji17emoji16emoji15emoji14emoji13emoji12emoji11
                <u>${command.reactions.caption.byall}:</u> 550(275 ${command.reactions.customreaction})
                emoji30emoji29emoji28emoji27emoji26emoji25emoji24emoji23emoji22emoji21
                """;
        final String username = "username";
        BotRequest request = TestUtils.getRequestFromGroup("today " + username);
        Chat chat = request.getMessage().getChat();
        User user = request.getMessage().getUser();

        when(userService.get(username)).thenReturn(user);
        when(reactionsStatsService.get(chat, user)).thenReturn(getSomeReactionsStats());
        when(customReactionsStatsService.get(chat, user)).thenReturn(getSomeCustomReactionsStats());

        ReflectionTestUtils.invokeMethod(reactions, "postConstruct");

        BotResponse response = reactions.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponse, textResponse.getText());
    }

    @Test
    void analyzePrivateMessageTest() {
        BotRequest request = TestUtils.getRequestFromPrivate("test");

        List<BotResponse> botResponseList = reactions.analyze(request);

        verify(messageStatsService, never()).incrementReplies(any(org.telegram.bot.domain.entities.Message.class));
        verify(messageStatsService, never()).incrementReactions(any(org.telegram.bot.domain.entities.Message.class), anyInt());

        assertTrue(botResponseList.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = MessageKind.class, mode = EnumSource.Mode.INCLUDE, names = {"CALLBACK", "EDIT"})
    void analyzeUnsupportedKindOfMessageTest(MessageKind messageKind) {
        BotRequest request = TestUtils.getRequestFromGroup("test");
        request.getMessage().setMessageKind(messageKind);

        List<BotResponse> botResponseList = reactions.analyze(request);

        verify(messageStatsService, never()).incrementReplies(any(org.telegram.bot.domain.entities.Message.class));
        verify(messageStatsService, never()).incrementReactions(any(org.telegram.bot.domain.entities.Message.class), anyInt());

        assertTrue(botResponseList.isEmpty());
    }

    @Test
    void analyzeMessageWithoutRepliesOrReactionsTest() {
        BotRequest request = TestUtils.getRequestFromGroup("test");

        List<BotResponse> botResponseList = reactions.analyze(request);

        verify(messageStatsService, never()).incrementReplies(any(org.telegram.bot.domain.entities.Message.class));
        verify(messageStatsService, never()).incrementReactions(any(org.telegram.bot.domain.entities.Message.class), anyInt());

        assertTrue(botResponseList.isEmpty());
    }

    @Test
    void analyzeMessageHasReplyToTest() {
        BotRequest request = TestUtils.getRequestWithRepliedMessage("test");
        org.telegram.bot.domain.entities.Message incrementingRepliesMessage = new org.telegram.bot.domain.entities.Message();
        when(messageService.get(request.getMessage().getReplyToMessage().getMessageId())).thenReturn(incrementingRepliesMessage);

        List<BotResponse> botResponseList = reactions.analyze(request);

        verify(messageStatsService).incrementReplies(any(org.telegram.bot.domain.entities.Message.class));
        verify(messageStatsService, never()).incrementReactions(any(org.telegram.bot.domain.entities.Message.class), anyInt());

        assertTrue(botResponseList.isEmpty());
    }

    @Test
    void analyzeMessageHasReactionsToUnknownMessageTest() {
        BotRequest request = TestUtils.getRequestFromGroup("test");

        org.telegram.bot.domain.model.request.Reactions incomingReactions = new org.telegram.bot.domain.model.request.Reactions();
        incomingReactions.getNewEmojis().add("\uD83D\uDC4D");
        incomingReactions.getNewCustomEmojisIds().add("customEmojiId");

        Message message = request.getMessage();
        message.setMessageContentType(MessageContentType.REACTION);
        message.setReactions(incomingReactions);

        List<BotResponse> botResponseList = reactions.analyze(request);

        verify(messageStatsService, never()).incrementReplies(any(org.telegram.bot.domain.entities.Message.class));
        verify(messageStatsService, never()).incrementReactions(any(org.telegram.bot.domain.entities.Message.class), anyInt());
        verify(reactionsStatsService, never()).update(any(Chat.class), any(User.class), anyList(), anyList());
        verify(customReactionsStatsService, never()).update(any(Chat.class), any(User.class), anyList(), anyList());

        assertTrue(botResponseList.isEmpty());
    }

    @Test
    void analyzeMessageHasSelfReactionsTest() {
        BotRequest request = TestUtils.getRequestFromGroup("test");

        org.telegram.bot.domain.model.request.Reactions incomingReactions = new org.telegram.bot.domain.model.request.Reactions();
        incomingReactions.getNewEmojis().add("\uD83D\uDC4D");
        incomingReactions.getNewCustomEmojisIds().add("customEmojiId");

        Message message = request.getMessage();
        message.setMessageContentType(MessageContentType.REACTION);
        message.setReactions(incomingReactions);
        org.telegram.bot.domain.entities.Message reactedMessage = new org.telegram.bot.domain.entities.Message()
                .setUser(message.getUser());

        when(messageService.get(message.getMessageId())).thenReturn(reactedMessage);

        List<BotResponse> botResponseList = reactions.analyze(request);

        verify(messageStatsService, never()).incrementReplies(any(org.telegram.bot.domain.entities.Message.class));
        verify(messageStatsService, never()).incrementReactions(any(org.telegram.bot.domain.entities.Message.class), anyInt());
        verify(reactionsStatsService, never()).update(any(Chat.class), any(User.class), anyList(), anyList());
        verify(customReactionsStatsService, never()).update(any(Chat.class), any(User.class), anyList(), anyList());

        assertTrue(botResponseList.isEmpty());
    }

    @Test
    void analyzeMessageHasReactionsTest() {
        BotRequest request = TestUtils.getRequestFromGroup("test");

        org.telegram.bot.domain.model.request.Reactions incomingReactions = new org.telegram.bot.domain.model.request.Reactions();
        incomingReactions.getNewEmojis().add("\uD83D\uDC4D");
        incomingReactions.getNewCustomEmojisIds().add("customEmojiId");

        Message message = request.getMessage();
        message.setMessageContentType(MessageContentType.REACTION);
        message.setReactions(incomingReactions);
        User anotherUser = TestUtils.getUser(TestUtils.ANOTHER_USER_ID);
        org.telegram.bot.domain.entities.Message reactedMessage = new org.telegram.bot.domain.entities.Message()
                .setUser(anotherUser);

        when(messageService.get(message.getMessageId())).thenReturn(reactedMessage);

        List<BotResponse> botResponseList = reactions.analyze(request);

        verify(messageStatsService, never()).incrementReplies(any(org.telegram.bot.domain.entities.Message.class));
        verify(messageStatsService).incrementReactions(reactedMessage, 2);
        verify(reactionsStatsService).update(message.getChat(), anotherUser, incomingReactions.getOldEmojis(), incomingReactions.getNewEmojis());
        verify(customReactionsStatsService).update(message.getChat(), anotherUser, incomingReactions.getOldCustomEmojisIds(), incomingReactions.getNewCustomEmojisIds());

        assertTrue(botResponseList.isEmpty());
    }

    private ReactionsStats getSomeReactionsStats() {
        return new ReactionsStats()
                .setReactionDayStatsList(IntStream.range(0, 11).mapToObj(this::getReactionDayStats).toList())
                .setReactionMonthStatsList(IntStream.range(11, 21).mapToObj(this::getReactionMonthStats).toList())
                .setReactionStatsList(IntStream.range(20, 31).mapToObj(this::getReactionStats).toList());
    }

    private ReactionStats getReactionStats(int i) {
        return new ReactionStats()
                .setChat(TestUtils.getChat())
                .setUser(getSomeUser(i))
                .setEmoji("emoji" + i)
                .setCount(i);
    }

    private ReactionMonthStats getReactionMonthStats(int i) {
        return new ReactionMonthStats()
                .setChat(TestUtils.getChat())
                .setUser(getSomeUser(i))
                .setEmoji("emoji" + i)
                .setCount(i);
    }

    private ReactionDayStats getReactionDayStats(int i) {
        return new ReactionDayStats()
                .setChat(TestUtils.getChat())
                .setUser(getSomeUser(i))
                .setEmoji("emoji" + i)
                .setCount(i);
    }

    private CustomReactionsStats getSomeCustomReactionsStats() {
        return new CustomReactionsStats()
                .setCustomReactionDayStats(IntStream.range(0, 11).mapToObj(this::getCustomReactionDayStats).toList())
                .setCustomReactionMonthStats(IntStream.range(11, 21).mapToObj(this::getCustomReactionMonthStats).toList())
                .setCustomReactionStats(IntStream.range(20, 31).mapToObj(this::getCustomReactionStats).toList());
    }

    private CustomReactionStats getCustomReactionStats(int i) {
        return new CustomReactionStats()
                .setChat(TestUtils.getChat())
                .setUser(getSomeUser(i))
                .setEmojiId("emoji" + i)
                .setCount(i);
    }

    private CustomReactionMonthStats getCustomReactionMonthStats(int i) {
        return new CustomReactionMonthStats()
                .setChat(TestUtils.getChat())
                .setUser(getSomeUser(i))
                .setEmojiId("emoji" + i)
                .setCount(i);
    }

    private CustomReactionDayStats getCustomReactionDayStats(int i) {
        return new CustomReactionDayStats()
                .setChat(TestUtils.getChat())
                .setUser(getSomeUser(i))
                .setEmojiId("emoji" + i)
                .setCount(i);
    }

    private User getSomeUser(int i) {
        if (isEven(i)) {
            return USER;
        } else {
            return ANOTHER_USER;
        }
    }

    private static boolean isEven(int num) {
        return num % 2 == 0;
    }

}