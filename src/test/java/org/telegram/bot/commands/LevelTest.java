package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LevelTest {

    @Mock
    private Bot bot;
    @Mock
    private UserService userService;
    @Mock
    private ChatService chatService;
    @Mock
    private SpeechService speechService;

    @InjectMocks
    private Level level;

    @Test
    void parseFromPrivateChatWithoutArgumentsTest() {
        Update update = TestUtils.getUpdateFromPrivate("level");

        assertThrows(BotException.class, () -> level.parse(update));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(update.getMessage().getChatId());
    }

    @Test
    void parseFromGroupChatWithoutArgumentsTest() {
        final String expectedResponseText = "${command.level.grouplevel} - 5";
        Update update = TestUtils.getUpdateFromGroup("level");
        Long chatId = update.getMessage().getChatId();

        when(chatService.getChatAccessLevel(chatId)).thenReturn(5);

        BotApiMethodMessage method = level.parse(update).get(0);

        SendMessage sendMessage = TestUtils.checkDefaultSendMessageParams(method);
        assertEquals(expectedResponseText, sendMessage.getText());
        verify(bot).sendTyping(chatId);
    }

    @Test
    void parseWithUnknownUsernameAsArgumentTest() {
        Update update = TestUtils.getUpdateFromGroup("level username");

        assertThrows(BotException.class, () -> level.parse(update));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(update.getMessage().getChatId());
    }

    @Test
    void parseWithUsernameAsArgumentTest() {
        final String expectedResponseText = "${command.level.userlevel} [username](tg://user?id=1) - 1";
        final String username = "username";
        Update update = TestUtils.getUpdateFromGroup("level " + username);
        Long chatId = update.getMessage().getChatId();

        when(userService.get(username)).thenReturn(TestUtils.getUser());

        BotApiMethodMessage method = level.parse(update).get(0);

        SendMessage sendMessage = TestUtils.checkDefaultSendMessageParams(method);
        assertEquals(expectedResponseText, sendMessage.getText());
        verify(bot).sendTyping(chatId);
    }

    @Test
    void parseWithNumberAsArgumentTest() {
        final String expectedResponseText = "saved";
        final int expectedChatLevel = 5;
        Update update = TestUtils.getUpdateFromGroup("level " + expectedChatLevel);
        Long chatId = update.getMessage().getChatId();

        when(chatService.get(chatId)).thenReturn(TestUtils.getChat(chatId));
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);

        BotApiMethodMessage method = level.parse(update).get(0);

        SendMessage sendMessage = TestUtils.checkDefaultSendMessageParams(method);
        assertEquals(expectedResponseText, sendMessage.getText());

        ArgumentCaptor<Chat> chatArgumentCaptor = ArgumentCaptor.forClass(Chat.class);
        verify(chatService).save(chatArgumentCaptor.capture());

        Chat savedChat = chatArgumentCaptor.getValue();
        assertEquals(chatId, savedChat.getChatId());
        assertEquals(expectedChatLevel, savedChat.getAccessLevel());

        verify(bot).sendTyping(chatId);
    }

    @Test
    void parseWithUsernameAndWrongLevelAsArgumentTest() {
        Update update = TestUtils.getUpdateFromGroup("level username test");

        assertThrows(BotException.class, () -> level.parse(update));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(update.getMessage().getChatId());
    }

    @Test
    void parseWithUnknownUsernameAndLevelAsArgumentTest() {
        Update update = TestUtils.getUpdateFromGroup("level username 5");

        assertThrows(BotException.class, () -> level.parse(update));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(update.getMessage().getChatId());
    }

    @Test
    void parseWithUsernameAndLevelAsArgumentTest() {
        final String expectedResponseText = "saved";
        final String username = "username";
        final int expectedUserLevel = 5;
        Update update = TestUtils.getUpdateFromGroup("level " + username + " " + expectedUserLevel);
        User updatedUser = TestUtils.getUser();

        when(userService.get(username)).thenReturn(updatedUser);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);

        BotApiMethodMessage method = level.parse(update).get(0);

        SendMessage sendMessage = TestUtils.checkDefaultSendMessageParams(method);
        assertEquals(expectedResponseText, sendMessage.getText());

        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userArgumentCaptor.capture());

        User savedUser = userArgumentCaptor.getValue();
        assertEquals(updatedUser.getUserId(), savedUser.getUserId());
        assertEquals(expectedUserLevel, savedUser.getAccessLevel());

        verify(bot).sendTyping(update.getMessage().getChatId());
    }

}