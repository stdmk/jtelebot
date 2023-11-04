package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.telegram.bot.Bot;
import org.telegram.bot.commands.Alias;
import org.telegram.bot.commands.Echo;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
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
    private Echo echo;

    @InjectMocks
    private Alias alias;

    @Test
    void parseWithUnknownAliasNameTest() {
        Update update = getUpdateFromGroup("alias test");

        when(aliasService.get(any(org.telegram.bot.domain.entities.Chat.class), any(org.telegram.bot.domain.entities.User.class), anyString()))
                .thenReturn(null);

        assertThrows((BotException.class), () -> alias.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
    }

    @Test
    void parseWithAliasNameTest() {
        final String expectedResponseText = "1. test - `echo`";
        org.telegram.bot.domain.entities.Alias aliasEntity = getSomeAliasEntity();
        Update update = getUpdateFromGroup("alias test");

        when(aliasService.get(any(org.telegram.bot.domain.entities.Chat.class), any(org.telegram.bot.domain.entities.User.class), anyString()))
                .thenReturn(aliasEntity);

        SendMessage sendMessage = alias.parse(update);
        checkDefaultSendMessageParams(sendMessage, true, ParseMode.MARKDOWN);

        String actualResponseText = sendMessage.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void parseTest() {
        org.telegram.bot.domain.entities.Alias aliasEntity = getSomeAliasEntity();
        final String expectedResponseText = "*${command.alias.aliaslist}:*\n1. test - `echo`";

        when(aliasService.getByChatAndUser(any(org.telegram.bot.domain.entities.Chat.class), any(org.telegram.bot.domain.entities.User.class)))
                .thenReturn(List.of(aliasEntity));

        SendMessage sendMessage = alias.parse(getUpdateFromGroup());
        checkDefaultSendMessageParams(sendMessage, true, ParseMode.MARKDOWN);

        String actualResponseText = sendMessage.getText();
        assertEquals(expectedResponseText, actualResponseText);
    }

    @Test
    void analyzeTest() {
        org.telegram.bot.domain.entities.Alias aliasEntity = getSomeAliasEntity();
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

        assertDoesNotThrow(() -> alias.analyze(echo, getUpdateFromGroup()));

        verify(userStatsService).incrementUserStatsCommands(any(org.telegram.bot.domain.entities.Chat.class), any(org.telegram.bot.domain.entities.User.class));
        verify(context).getBean(anyString());
    }

    private org.telegram.bot.domain.entities.Alias getSomeAliasEntity() {
        return new org.telegram.bot.domain.entities.Alias()
                .setId(1L)
                .setChat(new Chat().setChatId(-1L))
                .setUser(new User().setUserId(1L))
                .setName("test")
                .setValue("echo");
    }

}