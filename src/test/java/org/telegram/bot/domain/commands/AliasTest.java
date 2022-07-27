package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageImpl;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Alias;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(classes = {TestConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AliasTest {

    @MockBean
    ApplicationContext context;
    @MockBean
    BotStats botStats;
    @MockBean
    AliasService aliasService;
    @MockBean
    ChatService chatService;
    @MockBean
    UserService userService;
    @MockBean
    UserStatsService userStatsService;
    @MockBean
    CommandPropertiesService commandPropertiesService;

    @Autowired
    User user;
    @Autowired
    Chat chat;
    @Autowired
    Update update;

    org.telegram.bot.domain.commands.Alias aliasCommand;

    @BeforeAll
    void init() {
        aliasCommand = new org.telegram.bot.domain.commands.Alias(context, botStats, aliasService, userService, userStatsService, commandPropertiesService);

        Alias savedAlias = new Alias();
        savedAlias.setId(1L);
        savedAlias.setChat(chat);
        savedAlias.setUser(user);
        savedAlias.setName("Test");
        savedAlias.setValue("echo");

        Mockito.when(chatService.get(any(Long.class))).thenReturn(chat);
        Mockito.when(userService.get(any(Long.class))).thenReturn(user);
        Mockito.when(aliasService.getByChatAndUser(any(Chat.class), any(User.class), anyInt()))
                .thenReturn(new PageImpl<>(Collections.singletonList(savedAlias)));
    }

    @Test
    void parseTest() {
        SendMessage sendMessage = aliasCommand.parse(update);
        assertEquals("*Список твоих алиасов:*\n1. Test - `echo`\n", sendMessage.getText());
    }

    @Test
    void analyzeTest() {

    }
}