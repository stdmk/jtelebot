package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.bot.utils.ObjectCopier;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.telegram.bot.TestUtils.ANOTHER_USER_ID;

@ExtendWith(MockitoExtension.class)
class KarmaTest {

    @Mock
    private Bot bot;
    @Mock
    private ObjectCopier objectCopier;
    @Mock
    private CommandPropertiesService commandPropertiesService;
    @Mock
    private SpeechService speechService;
    @Mock
    private UserService userService;
    @Mock
    private UserStatsService userStatsService;

    @InjectMocks
    private Karma karma;

    @Test
    void parsePrivateMessageTest() {
        BotRequest request = TestUtils.getRequestFromPrivate("");

        assertThrows(BotException.class, () -> karma.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void getKarmaOfUserTest() {
        final String expectedResponseText = """
                <b><a href="tg://user?id=1">username</a></b>
                \uD83D\uDE07${command.karma.caption}: <b>33</b> (35)
                ❤️${command.karma.kindness}: <b>36</b> (38)
                \uD83D\uDC94${command.karma.wickedness}: <b>39</b> (41)
                """;
        BotRequest request = TestUtils.getRequestFromGroup();

        when(userStatsService.get(any(Chat.class), any(User.class))).thenReturn(getSomeUserStats());

        BotResponse response = karma.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void getKarmaOfAnotherUser() {
        final String expectedResponseText = """
                <b><a href="tg://user?id=1">username</a></b>
                \uD83D\uDE08${command.karma.caption}: <b>-1</b> (35)
                ❤️${command.karma.kindness}: <b>36</b> (38)
                \uD83D\uDC94${command.karma.wickedness}: <b>39</b> (41)
                """;
        BotRequest request = TestUtils.getRequestWithRepliedMessage("");

        UserStats userStats = getSomeUserStats();
        userStats.setNumberOfKarma(-1);
        when(userStatsService.get(any(Chat.class), any(User.class))).thenReturn(userStats);

        BotResponse response = karma.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void changeKarmaWithoutValueTest() {
        BotRequest request = TestUtils.getRequestFromGroup("karma @username");

        assertThrows(BotException.class, () -> karma.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void changeKarmaWithCorruptedValueTest() {
        BotRequest request = TestUtils.getRequestFromGroup("karma @username one");

        assertThrows(BotException.class, () -> karma.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, 0, 2})
    void changeKarmaWithWrongValueTest() {
        BotRequest request = TestUtils.getRequestFromGroup("karma @username 2");

        assertThrows(BotException.class, () -> karma.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void changeKarmaOfUnknownUserTest() {
        BotRequest request = TestUtils.getRequestFromGroup("karma @username 1");

        when(userService.get(anyString())).thenReturn(null);

        assertThrows(BotException.class, () -> karma.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void changeKarmaOfMyselfTest() {
        BotRequest request = TestUtils.getRequestFromGroup("karma @username 1");

        when(userService.get(anyString())).thenReturn(TestUtils.getUser());

        assertThrows(BotException.class, () -> karma.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @ParameterizedTest
    @MethodSource("provideKarmaChanges")
    void changeKarmaOfAnotherUserTest(int value, String expectedResponseText) {
        BotRequest request = TestUtils.getRequestFromGroup("karma " + ANOTHER_USER_ID + " " + value);

        when(userService.get(ANOTHER_USER_ID)).thenReturn(TestUtils.getUser(ANOTHER_USER_ID));
        when(userStatsService.get(any(Chat.class), any(User.class))).thenReturn(getSomeUserStats());

        BotResponse response = karma.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    private static Stream<Arguments> provideKarmaChanges() {
        return Stream.of(
                Arguments.of(1, "${command.karma.userskarma} <b><a href=\"tg://user?id=2\">username</a></b> ${command.karma.increased} \uD83D\uDC4D ${command.karma.changedto} <b>34</b>"),
                Arguments.of(-1, "${command.karma.userskarma} <b><a href=\"tg://user?id=2\">username</a></b> ${command.karma.reduced} \uD83D\uDC4E ${command.karma.changedto} <b>32</b>")
        );
    }

    private UserStats getSomeUserStats() {
        return new UserStats()
                .setId(1L)
                .setUser(TestUtils.getUser())
                .setChat(TestUtils.getChat())
                .setNumberOfMessages(2)
                .setNumberOfMessagesPerDay(3)
                .setNumberOfAllMessages(4L)
                .setNumberOfStickers(5)
                .setNumberOfStickersPerDay(6)
                .setNumberOfAllStickers(7L)
                .setNumberOfPhotos(8)
                .setNumberOfPhotosPerDay(9)
                .setNumberOfAllPhotos(10L)
                .setNumberOfAnimations(11)
                .setNumberOfAnimationsPerDay(12)
                .setNumberOfAllAnimations(13L)
                .setNumberOfAudio(14)
                .setNumberOfAudioPerDay(15)
                .setNumberOfAllAudio(16L)
                .setNumberOfDocuments(17)
                .setNumberOfDocumentsPerDay(18)
                .setNumberOfAllDocuments(19L)
                .setNumberOfVideos(20)
                .setNumberOfVideosPerDay(21)
                .setNumberOfAllVideos(22L)
                .setNumberOfVideoNotes(23)
                .setNumberOfVideoNotesPerDay(24)
                .setNumberOfAllVideoNotes(25L)
                .setNumberOfVoices(26)
                .setNumberOfVoicesPerDay(27)
                .setNumberOfAllVoices(28L)
                .setNumberOfCommands(29)
                .setNumberOfCommandsPerDay(30)
                .setNumberOfAllCommands(31L)
                .setNumberOfKarma(33)
                .setNumberOfKarmaPerDay(34)
                .setNumberOfAllKarma(35L)
                .setNumberOfGoodness(36)
                .setNumberOfGoodnessPerDay(37)
                .setNumberOfAllGoodness(38L)
                .setNumberOfWickedness(39)
                .setNumberOfWickednessPerDay(40)
                .setNumberOfAllWickedness(41L);
    }

    @Test
    void analyzeWithoutTextTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        List<BotResponse> botResponseList = karma.analyze(request);
        assertTrue(botResponseList.isEmpty());
    }

    @Test
    void analyzeWithoutRepliedMessageTest() {
        BotRequest request = TestUtils.getRequestFromGroup("--");
        List<BotResponse> botResponseList = karma.analyze(request);
        assertTrue(botResponseList.isEmpty());
    }

    @Test
    void analyzeWithoutAccessCommandTest() {
        BotRequest request = TestUtils.getRequestWithRepliedMessage("");
        request.getMessage().setText("--");

        CommandProperties commandProperties = mock(CommandProperties.class);
        when(commandProperties.getAccessLevel()).thenReturn(1);
        when(commandPropertiesService.getCommand(Karma.class)).thenReturn(commandProperties);
        when(userService.getCurrentAccessLevel(TestUtils.DEFAULT_USER_ID, TestUtils.DEFAULT_CHAT_ID))
                .thenReturn(AccessLevel.NEWCOMER);
        when(userService.isUserHaveAccessForCommand(anyInt(), anyInt())).thenReturn(false);

        List<BotResponse> botResponseList = karma.analyze(request);

        assertTrue(botResponseList.isEmpty());
    }

    @Test
    void analyzeWithCopyingrequestFailedTest() {
        BotRequest request = TestUtils.getRequestWithRepliedMessage("");
        request.getMessage().setText("--");

        CommandProperties commandProperties = mock(CommandProperties.class);
        when(commandProperties.getAccessLevel()).thenReturn(1);
        when(commandPropertiesService.getCommand(Karma.class)).thenReturn(commandProperties);
        when(userService.getCurrentAccessLevel(TestUtils.DEFAULT_USER_ID, TestUtils.DEFAULT_CHAT_ID))
                .thenReturn(AccessLevel.FAMILIAR);
        when(userService.isUserHaveAccessForCommand(anyInt(), anyInt())).thenReturn(true);

        List<BotResponse> botResponseList = karma.analyze(request);

        assertTrue(botResponseList.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("provideMessageTexts")
    void analyzeTest(String messageText, String expectedCommand) {
        BotRequest request = TestUtils.getRequestWithRepliedMessage("");
        request.getMessage().setText(messageText);

        CommandProperties commandProperties = mock(CommandProperties.class);
        when(commandProperties.getAccessLevel()).thenReturn(1);
        when(commandProperties.getCommandName()).thenReturn("karma");
        when(commandPropertiesService.getCommand(Karma.class)).thenReturn(commandProperties);
        when(userService.getCurrentAccessLevel(TestUtils.DEFAULT_USER_ID, TestUtils.DEFAULT_CHAT_ID))
                .thenReturn(AccessLevel.FAMILIAR);
        when(userService.isUserHaveAccessForCommand(anyInt(), anyInt())).thenReturn(true);
        when(objectCopier.copyObject(any(BotRequest.class), ArgumentMatchers.any())).thenReturn(request);
        when(userService.get(ANOTHER_USER_ID)).thenReturn(TestUtils.getUser(ANOTHER_USER_ID));
        when(userStatsService.get(any(Chat.class), any(User.class))).thenReturn(getSomeUserStats());

        BotResponse botResponse = karma.analyze(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedCommand, textResponse.getText());
    }

    private static Stream<Arguments> provideMessageTexts() {
        final String increaseKarmaCommand = "${command.karma.userskarma} <b><a href=\"tg://user?id=" + ANOTHER_USER_ID +"\">username</a></b> ${command.karma.increased} \uD83D\uDC4D ${command.karma.changedto} <b>34</b>";
        final String decreaseKarmaCommand = "${command.karma.userskarma} <b><a href=\"tg://user?id=" + ANOTHER_USER_ID + "\">username</a></b> ${command.karma.reduced} \uD83D\uDC4E ${command.karma.changedto} <b>32</b>";

        Stream<Arguments> increaseArgumentsStream = Stream.of("👍", "👍🏻", "👍🏼", "👍🏽", "👍🏾", "👍🏿", "+1", "++")
                .map(text -> Arguments.of(text, increaseKarmaCommand));

        Stream<Arguments> decreaseArgumentsStream = Stream.of("👎🏿", "👎🏾", "👎🏽", "👎🏼", "👎🏻", "👎", "-1", "--")
                .map(text -> Arguments.of(text, decreaseKarmaCommand));

        return Stream.concat(increaseArgumentsStream, decreaseArgumentsStream);
    }

}