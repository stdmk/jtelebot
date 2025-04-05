package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TopTest {

    @Mock
    private Bot bot;
    @Mock
    private UserStatsService userStatsService;
    @Mock
    private UserService userService;
    @Mock
    private SpeechService speechService;
    @Mock
    private LanguageResolver languageResolver;
    @Mock
    private InternationalizationService internationalizationService;

    @InjectMocks
    private Top top;

    @Test
    void parseWithoutArgumentsTest() {
        final String expectedResponseText = """
                <b><a href="tg://user?id=1">username</a></b>
                <u>${command.top.permonth}:</u>
                📧${command.top.userstats.messages}: <b>1</b>
                😇${command.top.userstats.karma}: <b>1</b>
                ❤️${command.top.userstats.kindness}: <b>1</b>
                💔${command.top.userstats.wickedness}: <b>1</b>
                🖼${command.top.userstats.stickers}: <b>1</b>
                📷${command.top.userstats.images}: <b>1</b>
                🎞${command.top.userstats.animations}: <b>1</b>
                🎵${command.top.userstats.music}: <b>1</b>
                📄${command.top.userstats.documents}: <b>1</b>
                🎥${command.top.userstats.videos}: <b>1</b>
                📼${command.top.userstats.videomessages}: <b>1</b>
                ▶${command.top.userstats.voices}: <b>1</b>
                😁${command.top.userstats.reactions}: <b>1</b>
                🤖${command.top.userstats.commands}: <b>1</b>
                
                <u>${command.top.userstats.total}:</u>
                📧${command.top.userstats.messages}: <b>1</b>
                😇${command.top.userstats.karma}: <b>1</b>
                ❤️${command.top.userstats.kindness}: <b>1</b>
                💔${command.top.userstats.wickedness}: <b>1</b>
                🖼${command.top.userstats.stickers}: <b>1</b>
                📷${command.top.userstats.images}: <b>1</b>
                🎞${command.top.userstats.animations}: <b>1</b>
                🎵${command.top.userstats.music}: <b>1</b>
                📄${command.top.userstats.documents}: <b>1</b>
                🎥${command.top.userstats.videos}: <b>1</b>
                📼${command.top.userstats.videomessages}: <b>1</b>
                ▶${command.top.userstats.voices}: <b>1</b>
                😁${command.top.userstats.reactions}: <b>1</b>
                🤖${command.top.userstats.commands}: <b>1</b>
                """;
        BotRequest request = TestUtils.getRequestFromGroup("top");
        Message message = request.getMessage();

        when(userStatsService.get(eq(message.getChat()), any(User.class))).thenReturn(getSomeUserStats(message.getUser()));

        BotResponse botResponse = top.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithoutArgumentsAndWithRepliedMessageUnknownUserTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestWithRepliedMessage("top");

        when(speechService.getRandomMessageByTag(BotSpeechTag.USER_NOT_FOUND)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> top.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithoutArgumentsAndWithRepliedMessageTest() {
        final String expectedResponseText = """
                <b><a href="tg://user?id=1">username</a></b>
                <u>${command.top.permonth}:</u>
                📧${command.top.userstats.messages}: <b>1</b>
                😇${command.top.userstats.karma}: <b>1</b>
                ❤️${command.top.userstats.kindness}: <b>1</b>
                💔${command.top.userstats.wickedness}: <b>1</b>
                🖼${command.top.userstats.stickers}: <b>1</b>
                📷${command.top.userstats.images}: <b>1</b>
                🎞${command.top.userstats.animations}: <b>1</b>
                🎵${command.top.userstats.music}: <b>1</b>
                📄${command.top.userstats.documents}: <b>1</b>
                🎥${command.top.userstats.videos}: <b>1</b>
                📼${command.top.userstats.videomessages}: <b>1</b>
                ▶${command.top.userstats.voices}: <b>1</b>
                😁${command.top.userstats.reactions}: <b>1</b>
                🤖${command.top.userstats.commands}: <b>1</b>
                
                <u>${command.top.userstats.total}:</u>
                📧${command.top.userstats.messages}: <b>1</b>
                😇${command.top.userstats.karma}: <b>1</b>
                ❤️${command.top.userstats.kindness}: <b>1</b>
                💔${command.top.userstats.wickedness}: <b>1</b>
                🖼${command.top.userstats.stickers}: <b>1</b>
                📷${command.top.userstats.images}: <b>1</b>
                🎞${command.top.userstats.animations}: <b>1</b>
                🎵${command.top.userstats.music}: <b>1</b>
                📄${command.top.userstats.documents}: <b>1</b>
                🎥${command.top.userstats.videos}: <b>1</b>
                📼${command.top.userstats.videomessages}: <b>1</b>
                ▶${command.top.userstats.voices}: <b>1</b>
                😁${command.top.userstats.reactions}: <b>1</b>
                🤖${command.top.userstats.commands}: <b>1</b>
                """;
        BotRequest request = TestUtils.getRequestWithRepliedMessage("top");
        Message message = request.getMessage();

        when(userStatsService.get(eq(message.getChat()), any(User.class))).thenReturn(getSomeUserStats(message.getUser()));

        BotResponse botResponse = top.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithUsernameAsArgumentTest() {
        final String expectedResponseText = """
                <b><a href="tg://user?id=1">username</a></b>
                <u>${command.top.permonth}:</u>
                📧${command.top.userstats.messages}: <b>1</b>
                😇${command.top.userstats.karma}: <b>1</b>
                ❤️${command.top.userstats.kindness}: <b>1</b>
                💔${command.top.userstats.wickedness}: <b>1</b>
                🖼${command.top.userstats.stickers}: <b>1</b>
                📷${command.top.userstats.images}: <b>1</b>
                🎞${command.top.userstats.animations}: <b>1</b>
                🎵${command.top.userstats.music}: <b>1</b>
                📄${command.top.userstats.documents}: <b>1</b>
                🎥${command.top.userstats.videos}: <b>1</b>
                📼${command.top.userstats.videomessages}: <b>1</b>
                ▶${command.top.userstats.voices}: <b>1</b>
                😁${command.top.userstats.reactions}: <b>1</b>
                🤖${command.top.userstats.commands}: <b>1</b>
                
                <u>${command.top.userstats.total}:</u>
                📧${command.top.userstats.messages}: <b>1</b>
                😇${command.top.userstats.karma}: <b>1</b>
                ❤️${command.top.userstats.kindness}: <b>1</b>
                💔${command.top.userstats.wickedness}: <b>1</b>
                🖼${command.top.userstats.stickers}: <b>1</b>
                📷${command.top.userstats.images}: <b>1</b>
                🎞${command.top.userstats.animations}: <b>1</b>
                🎵${command.top.userstats.music}: <b>1</b>
                📄${command.top.userstats.documents}: <b>1</b>
                🎥${command.top.userstats.videos}: <b>1</b>
                📼${command.top.userstats.videomessages}: <b>1</b>
                ▶${command.top.userstats.voices}: <b>1</b>
                😁${command.top.userstats.reactions}: <b>1</b>
                🤖${command.top.userstats.commands}: <b>1</b>
                """;
        final String username = "username";
        BotRequest request = TestUtils.getRequestWithRepliedMessage("");
        Message message = request.getMessage();
        message.setText("top " + username);

        when(userService.get(username)).thenReturn(message.getUser());
        when(userStatsService.get(eq(message.getChat()), any(User.class))).thenReturn(getSomeUserStats(message.getUser()));

        BotResponse botResponse = top.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithUnknownParamAsArgumentTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("top tratatam-tratatam");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> top.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @ParameterizedTest
    @MethodSource("provideParams")
    void parseWithParamAsArgumentTest(String param, String expectedResponseText) {
        BotRequest request = TestUtils.getRequestFromGroup("top " + param);

        setParams();

        when(userStatsService.getSortedUserStatsListWithKarmaForChat(any(Chat.class), anyString(), anyInt(), anyBoolean()))
                .thenReturn(Stream.of(0, 30, 60, 90, 120, 150).map(this::getSomeUserStats).toList());
        when(userStatsService.getSortedUserStatsListForChat(any(Chat.class), anyString(), anyInt()))
                .thenReturn(Stream.of(0, 30, 60, 90, 120, 150).map(this::getSomeUserStats).toList());

        BotResponse botResponse = top.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void getTopByChatTest() throws InvocationTargetException, IllegalAccessException {
        final String expectedResponseText = """
            <b>${command.top.list.caption} monthly:</b>
            <a href="tg://user?id=-30">@</a> <code>1) -30 username-30</code>
            <a href="tg://user?id=30">@</a> <code>2) 30  username30</code>
            <a href="tg://user?id=60">@</a> <code>3) 60  username60</code>
            <a href="tg://user?id=90">@</a> <code>4) 90  username90</code>
            <a href="tg://user?id=120">@</a> <code>5) 120 username120</code>
            <a href="tg://user?id=150">@</a> <code>6) 150 username150</code>
            ${command.top.list.totalcaption}: <b>420</b>
            ${command.top.monthlyclearcaption}""";
        setParams();

        final String lang = "en";
        Chat chat = TestUtils.getChat();
        when(languageResolver.getChatLanguageCode(chat)).thenReturn(lang);
        when(internationalizationService.internationalize("${command.top.list.monthly}", lang)).thenReturn("monthly");
        when(userStatsService.getSortedUserStatsListForChat(any(Chat.class), anyString(), anyInt()))
                .thenReturn(Stream.of(-30, 0, 30, 60, 90, 120, 150).map(this::getSomeUserStats).toList());

        TextResponse textResponse = top.getTopByChat(chat);
        TestUtils.checkDefaultTextResponseParams(textResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    private void setParams() {
        when(internationalizationService.getAllTranslations("command.top.list.daily")).thenReturn(java.util.Set.of("all", "total"));
        when(internationalizationService.getAllTranslations("command.top.list.monthly")).thenReturn(java.util.Set.of("daily"));
        when(internationalizationService.getAllTranslations("command.top.list.total")).thenReturn(java.util.Set.of("monthly"));
        when(internationalizationService.getAllTranslations("command.top.list.karma")).thenReturn(java.util.Set.of("karma"));
        when(internationalizationService.getAllTranslations("command.top.list.stickers")).thenReturn(java.util.Set.of("sticker", "stickers"));
        when(internationalizationService.getAllTranslations("command.top.list.photos")).thenReturn(java.util.Set.of("photo", "photos", "image", "images"));
        when(internationalizationService.getAllTranslations("command.top.list.animations")).thenReturn(java.util.Set.of("animation", "animations"));
        when(internationalizationService.getAllTranslations("command.top.list.audio")).thenReturn(java.util.Set.of("audio", "music"));
        when(internationalizationService.getAllTranslations("command.top.list.documents")).thenReturn(java.util.Set.of("document", "documents"));
        when(internationalizationService.getAllTranslations("command.top.list.videos")).thenReturn(java.util.Set.of("video", "videos"));
        when(internationalizationService.getAllTranslations("command.top.list.videonotes")).thenReturn(java.util.Set.of("videonote", "videonotes"));
        when(internationalizationService.getAllTranslations("command.top.list.voices")).thenReturn(java.util.Set.of("voice", "voices"));
        when(internationalizationService.getAllTranslations("command.top.list.commands")).thenReturn(java.util.Set.of("command", "commands"));
        when(internationalizationService.getAllTranslations("command.top.list.goodness")).thenReturn(java.util.Set.of("good", "goodness", "kindness"));
        when(internationalizationService.getAllTranslations("command.top.list.wickedness")).thenReturn(java.util.Set.of("wicked", "wickedness"));
        when(internationalizationService.getAllTranslations("command.top.list.reactions")).thenReturn(java.util.Set.of("reaction", "reactions"));

        ReflectionTestUtils.invokeMethod(top, "postConstruct");
    }

    private static Stream<Arguments> provideParams() {
        final String responseTextTemplate = """
                <b>${command.top.list.caption} %s:</b>
                <a href="tg://user?id=30">@</a> <code>1) 30  username30</code>
                <a href="tg://user?id=60">@</a> <code>2) 60  username60</code>
                <a href="tg://user?id=90">@</a> <code>3) 90  username90</code>
                <a href="tg://user?id=120">@</a> <code>4) 120 username120</code>
                <a href="tg://user?id=150">@</a> <code>5) 150 username150</code>
                ${command.top.list.totalcaption}: <b>450</b>""";

        return Stream.of(
                Arguments.of("all", String.format(responseTextTemplate, "all")),
                Arguments.of("daily", String.format(responseTextTemplate, "daily")),
                Arguments.of("monthly", String.format(responseTextTemplate, "monthly")),
                Arguments.of("karma", String.format(responseTextTemplate, "karma")),
                Arguments.of("sticker", String.format(responseTextTemplate, "sticker")),
                Arguments.of("photo", String.format(responseTextTemplate, "photo")),
                Arguments.of("animation", String.format(responseTextTemplate, "animation")),
                Arguments.of("audio", String.format(responseTextTemplate, "audio")),
                Arguments.of("document", String.format(responseTextTemplate, "document")),
                Arguments.of("video", String.format(responseTextTemplate, "video")),
                Arguments.of("videonote", String.format(responseTextTemplate, "videonote")),
                Arguments.of("voice", String.format(responseTextTemplate, "voice")),
                Arguments.of("reactions", String.format(responseTextTemplate, "reactions")),
                Arguments.of("command", String.format(responseTextTemplate, "command")),
                Arguments.of("good", String.format(responseTextTemplate, "good")),
                Arguments.of("wicked", String.format(responseTextTemplate, "wicked")),
                Arguments.of("sticker all", String.format(responseTextTemplate, "sticker all")),
                Arguments.of("sticker monthly", String.format(responseTextTemplate, "sticker monthly")),
                Arguments.of("sticker day", String.format(responseTextTemplate, "sticker day")),
                Arguments.of("karma monthly", String.format(responseTextTemplate, "karma monthly"))
        );
    }

    private UserStats getSomeUserStats(User user) {
        return getSomeUserStats(user, 1);
    }

    private UserStats getSomeUserStats(int i) {
        return getSomeUserStats(new User().setUserId((long) i).setUsername("username" + i), i);
    }

    private UserStats getSomeUserStats(User user, int i) {
        return new UserStats()
                .setNumberOfMessages(i)
                .setNumberOfKarma(i)
                .setNumberOfGoodness(i)
                .setNumberOfWickedness(i)
                .setNumberOfStickers(i)
                .setNumberOfPhotos(i)
                .setNumberOfAnimations(i)
                .setNumberOfAudio(i)
                .setNumberOfDocuments(i)
                .setNumberOfVideos(i)
                .setNumberOfVideoNotes(i)
                .setNumberOfVoices(i)
                .setNumberOfCommands(i)
                .setNumberOfReactions(i)
                .setNumberOfAllMessages((long) i)
                .setNumberOfAllKarma((long) i)
                .setNumberOfAllGoodness((long) i)
                .setNumberOfAllWickedness((long) i)
                .setNumberOfAllStickers((long) i)
                .setNumberOfAllPhotos((long) i)
                .setNumberOfAllAnimations((long) i)
                .setNumberOfAllAudio((long) i)
                .setNumberOfAllDocuments((long) i)
                .setNumberOfAllVideos((long) i)
                .setNumberOfAllVideoNotes((long) i)
                .setNumberOfAllVoices((long) i)
                .setNumberOfAllCommands((long) i)
                .setNumberOfAllReactions((long) i)
                .setNumberOfMessagesPerDay(i)
                .setNumberOfKarmaPerDay(i)
                .setNumberOfGoodnessPerDay(i)
                .setNumberOfWickednessPerDay(i)
                .setNumberOfStickersPerDay(i)
                .setNumberOfPhotosPerDay(i)
                .setNumberOfAnimationsPerDay(i)
                .setNumberOfAudioPerDay(i)
                .setNumberOfDocumentsPerDay(i)
                .setNumberOfVideosPerDay(i)
                .setNumberOfVideoNotesPerDay(i)
                .setNumberOfVoicesPerDay(i)
                .setNumberOfCommandsPerDay(i)
                .setNumberOfReactionsPerDay(i)
                .setUser(user);
    }

}