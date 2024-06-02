package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.ObjectCopier;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.telegram.bot.TestUtils.getRequestFromGroup;

@ExtendWith(MockitoExtension.class)
class AliasTest {

    @Mock
    private ApplicationContext context;
    @Mock
    private AliasService aliasService;
    @Mock
    private UserService userService;
    @Mock
    private UserStatsService userStatsService;
    @Mock
    private CommandPropertiesService commandPropertiesService;
    @Mock
    private SpeechService speechService;
    @Mock
    private Bot bot;
    @Mock
    private ObjectCopier objectCopier;
    @Mock
    private Echo echo;

    @InjectMocks
    private Alias alias;

    @Test
    void parseWithUnknownAliasNameTest() {
        BotRequest request = getRequestFromGroup("alias test");

        when(aliasService.get(any(org.telegram.bot.domain.entities.Chat.class), anyString()))
                .thenReturn(List.of());

        assertThrows((BotException.class), () -> alias.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
    }

    @Test
    void parseWithAliasNameTest() {
        final String expectedResponseText = "${command.alias.foundaliases}:\ntest1 — `echo1`\ntest2 — `echo2`";
        List<org.telegram.bot.domain.entities.Alias> aliasEntityList = getSomeAliasEntityList();
        BotRequest request = getRequestFromGroup("alias test");

        when(aliasService.get(any(org.telegram.bot.domain.entities.Chat.class), anyString())).thenReturn(aliasEntityList);

        BotResponse botResponse = alias.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse, true, FormattingStyle.MARKDOWN);

        String actualResponseText = textResponse.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void parseTest() {
        final String expectedResponseText = "*${command.alias.aliaslist}:*\ntest1 — `echo1`\ntest1 — `echo1`\ntest2 — `echo2`";
        List<org.telegram.bot.domain.entities.Alias> aliasEntityList = getSomeAliasEntityList();

        when(aliasService.getByChatAndUser(any(org.telegram.bot.domain.entities.Chat.class), any(org.telegram.bot.domain.entities.User.class)))
                .thenReturn(aliasEntityList);

        BotResponse botResponse = alias.parse(TestUtils.getRequestFromGroup()).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse, true, FormattingStyle.MARKDOWN);

        String actualResponseText = textResponse.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void analyzeTest() {
        final String expectedResponseText = "*${command.alias.aliaslist}:*\n";
        BotRequest request = TestUtils.getRequestFromGroup();
        org.telegram.bot.domain.entities.Alias aliasEntity = getSomeAliasEntityList().get(0);
        CommandProperties commandProperties = new CommandProperties().setClassName("Echo").setAccessLevel(0);

        when(aliasService.get(
                        any(org.telegram.bot.domain.entities.Chat.class),
                        any(org.telegram.bot.domain.entities.User.class),
                        anyString()))
                .thenReturn(aliasEntity);
        when(commandPropertiesService.findCommandInText(anyString(), anyString()))
                .thenReturn(commandProperties);
        when(userService.isUserHaveAccessForCommand(anyInt(), anyInt())).thenReturn(true);
        when(context.getBean(anyString())).thenReturn(echo);
        when(bot.getBotUsername()).thenReturn("jtelebot");
        when(userService.getCurrentAccessLevel(anyLong(), anyLong())).thenReturn(AccessLevel.NEWCOMER);
        when(objectCopier.copyObject(request, BotRequest.class)).thenReturn(request);
        when(echo.parse(request)).thenReturn(List.of(new TextResponse(request.getMessage()).setText(expectedResponseText)));

        BotResponse botResponse = alias.analyze(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        verify(userStatsService).incrementUserStatsCommands(any(org.telegram.bot.domain.entities.Chat.class), any(org.telegram.bot.domain.entities.User.class));

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void analyzeMultipleCommandsAliasTest() {
        final String echoResponseText = "${command.alias.foundaliases}:\n" +
                "test1 — `echo1`\n" +
                "test2 — `echo2`${command.alias.foundaliases}:\n";
        final String expectedResponseText = echoResponseText.repeat(3);
        BotRequest request = TestUtils.getRequestFromGroup();
        List<String> commands = List.of("say test1", "say test2", "say test3");
        org.telegram.bot.domain.entities.Alias aliasEntity = new org.telegram.bot.domain.entities.Alias()
                .setId(1L)
                .setChat(new Chat().setChatId(-1L))
                .setUser(new User().setUserId(1L))
                .setName("test")
                .setValue("{" + String.join(";", commands) + "}");
        CommandProperties commandProperties = new CommandProperties().setClassName("Echo").setAccessLevel(0);

        when(aliasService.get(
                any(org.telegram.bot.domain.entities.Chat.class),
                any(org.telegram.bot.domain.entities.User.class),
                anyString()))
                .thenReturn(aliasEntity);
        when(commandPropertiesService.findCommandInText(anyString(), anyString()))
                .thenReturn(commandProperties);
        when(userService.isUserHaveAccessForCommand(anyInt(), anyInt())).thenReturn(true);
        when(context.getBean(anyString())).thenReturn(echo);
        when(bot.getBotUsername()).thenReturn("jtelebot");
        when(userService.getCurrentAccessLevel(anyLong(), anyLong())).thenReturn(AccessLevel.NEWCOMER);
        when(objectCopier.copyObject(request, BotRequest.class)).thenReturn(request);
        when(echo.parse(request)).thenReturn(List.of(new TextResponse(request.getMessage()).setText(echoResponseText)));

        List<BotResponse> botResponseList = alias.analyze(request);
        List<TextResponse> textResponses = botResponseList.stream().map(TestUtils::checkDefaultTextResponseParams).collect(Collectors.toList());

        verify(userStatsService, times(3)).incrementUserStatsCommands(any(org.telegram.bot.domain.entities.Chat.class), any(org.telegram.bot.domain.entities.User.class));

        assertEquals(expectedResponseText, textResponses.stream().map(TextResponse::getText).collect(Collectors.joining()));
    }

    @Test
    void analyzeMultipleCommandsAliasWithArgumentTest() {
        final String echoResponseText = "${command.alias.foundaliases}:\n" +
                "test1 — `echo1`\n" +
                "test2 — `echo2`${command.alias.foundaliases}:\n";
        final String expectedResponseText = echoResponseText.repeat(3);
        BotRequest request = getRequestFromGroup("test hello");
        org.telegram.bot.domain.entities.Alias aliasEntity = new org.telegram.bot.domain.entities.Alias()
                .setId(1L)
                .setChat(new Chat().setChatId(-1L))
                .setUser(new User().setUserId(1L))
                .setName("test")
                .setValue("{say;say;say}");
        CommandProperties commandProperties = new CommandProperties().setClassName("Echo").setAccessLevel(0);

        when(aliasService.get(
                any(org.telegram.bot.domain.entities.Chat.class),
                any(org.telegram.bot.domain.entities.User.class),
                anyString()))
                .thenReturn(aliasEntity);
        when(commandPropertiesService.findCommandInText(anyString(), anyString()))
                .thenReturn(commandProperties);
        when(userService.isUserHaveAccessForCommand(anyInt(), anyInt())).thenReturn(true);
        when(context.getBean(anyString())).thenReturn(echo);
        when(bot.getBotUsername()).thenReturn("jtelebot");
        when(userService.getCurrentAccessLevel(anyLong(), anyLong())).thenReturn(AccessLevel.NEWCOMER);
        when(objectCopier.copyObject(request, BotRequest.class)).thenReturn(request);
        when(echo.parse(request)).thenReturn(List.of(new TextResponse(request.getMessage()).setText(echoResponseText)));

        List<BotResponse> botResponseList = alias.analyze(request);
        List<TextResponse> textResponses = botResponseList.stream().map(TestUtils::checkDefaultTextResponseParams).collect(Collectors.toList());

        verify(userStatsService, times(3)).incrementUserStatsCommands(any(org.telegram.bot.domain.entities.Chat.class), any(org.telegram.bot.domain.entities.User.class));

        assertEquals(expectedResponseText, textResponses.stream().map(TextResponse::getText).collect(Collectors.joining()));
    }

    private List<org.telegram.bot.domain.entities.Alias> getSomeAliasEntityList() {
        return List.of(
                new org.telegram.bot.domain.entities.Alias()
                    .setId(1L)
                    .setChat(new Chat().setChatId(-1L))
                    .setUser(new User().setUserId(1L))
                    .setName("test1")
                    .setValue("echo1"),
                new org.telegram.bot.domain.entities.Alias()
                        .setId(2L)
                        .setChat(new Chat().setChatId(-1L))
                        .setUser(new User().setUserId(2L))
                        .setName("test1")
                        .setValue("echo1"),
                new org.telegram.bot.domain.entities.Alias()
                        .setId(3L)
                        .setChat(new Chat().setChatId(-1L))
                        .setUser(new User().setUserId(1L))
                        .setName("test2")
                        .setValue("echo2"));
    }

}