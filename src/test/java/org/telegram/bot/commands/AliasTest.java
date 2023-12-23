package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.ObjectCopier;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.telegram.bot.TestUtils.checkDefaultSendMessageParams;
import static org.telegram.bot.TestUtils.getUpdateFromGroup;

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
        Update update = getUpdateFromGroup("alias test");

        when(aliasService.get(any(org.telegram.bot.domain.entities.Chat.class), anyString()))
                .thenReturn(List.of());

        assertThrows((BotException.class), () -> alias.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
    }

    @Test
    void parseWithAliasNameTest() {
        final String expectedResponseText = "${command.alias.foundaliases}:\ntest1 — `echo1`\ntest2 — `echo2`";
        List<org.telegram.bot.domain.entities.Alias> aliasEntityList = getSomeAliasEntityList();
        Update update = getUpdateFromGroup("alias test");

        when(aliasService.get(any(org.telegram.bot.domain.entities.Chat.class), anyString())).thenReturn(aliasEntityList);

        SendMessage sendMessage = alias.parse(update);
        checkDefaultSendMessageParams(sendMessage, true, ParseMode.MARKDOWN);

        String actualResponseText = sendMessage.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void parseTest() {
        final String expectedResponseText = "*${command.alias.aliaslist}:*\ntest1 — `echo1`\ntest1 — `echo1`\ntest2 — `echo2`";
        List<org.telegram.bot.domain.entities.Alias> aliasEntityList = getSomeAliasEntityList();

        when(aliasService.getByChatAndUser(any(org.telegram.bot.domain.entities.Chat.class), any(org.telegram.bot.domain.entities.User.class)))
                .thenReturn(aliasEntityList);

        SendMessage sendMessage = alias.parse(getUpdateFromGroup());
        checkDefaultSendMessageParams(sendMessage, true, ParseMode.MARKDOWN);

        String actualResponseText = sendMessage.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void analyzeTest() {
        Update update = getUpdateFromGroup();
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
        when(objectCopier.copyObject(update, Update.class)).thenReturn(update);

        assertDoesNotThrow(() -> alias.analyze(update));

        verify(userStatsService).incrementUserStatsCommands(any(org.telegram.bot.domain.entities.Chat.class), any(org.telegram.bot.domain.entities.User.class));
        verify(context).getBean(anyString());
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