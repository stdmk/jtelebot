package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AliasTest {

    @Mock
    private ApplicationContext context;
    @Mock
    private BotStats botStats;
    @Mock
    private AliasService aliasService;
    @Mock
    private UserService userService;
    @Mock
    private UserStatsService userStatsService;
    @Mock
    private CommandPropertiesService commandPropertiesService;
    @Mock
    private Bot bot;
    @Mock
    private Echo echo;

    @InjectMocks
    private Alias alias;

    @Test
    void parseTest() {
        org.telegram.bot.domain.entities.Alias aliasEntity = new org.telegram.bot.domain.entities.Alias()
                .setId(1L)
                .setChat(new Chat().setChatId(-1L))
                .setUser(new User().setUserId(1L))
                .setName("test")
                .setValue("echo");
        final String expectedResponseText = "*Список твоих алиасов:*\n1. test - `echo`\n";

        when(aliasService.getByChatAndUser(any(org.telegram.bot.domain.entities.Chat.class), any(org.telegram.bot.domain.entities.User.class)))
                .thenReturn(List.of(aliasEntity));

        SendMessage sendMessage = alias.parse(TestUtils.getUpdate());
        assertNotNull(sendMessage);

        String actualResponseText = sendMessage.getText();
        assertEquals(expectedResponseText, actualResponseText);

    }

    @Test
    void analyzeTest() {
        org.telegram.bot.domain.entities.Alias aliasEntity = new org.telegram.bot.domain.entities.Alias()
                .setId(1L)
                .setChat(new Chat().setChatId(-1L))
                .setUser(new User().setUserId(1L))
                .setName("test")
                .setValue("echo");
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

        assertDoesNotThrow(() -> alias.analyze(bot, echo, TestUtils.getUpdate()));

        verify(context).getBean(anyString());
    }

}