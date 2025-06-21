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
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private CommandPropertiesService commandPropertiesService;
    @Mock
    private SpeechService speechService;

    @InjectMocks
    private Level level;

    @Test
    void parseFromPrivateChatWithoutArgumentsTest() {
        BotRequest request = TestUtils.getRequestFromPrivate("level");

        assertThrows(BotException.class, () -> level.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseFromGroupChatWithoutArgumentsTest() {
        final String expectedResponseText = "${command.level.grouplevel} - 5";
        BotRequest request = TestUtils.getRequestFromGroup("level");
        Long chatId = request.getMessage().getChatId();

        when(chatService.getChatAccessLevel(chatId)).thenReturn(5);

        BotResponse response = level.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(chatId);
    }

    @Test
    void parseWithUnknownUsernameAsArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("level username");

        assertThrows(BotException.class, () -> level.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithUsernameAsArgumentTest() {
        final String expectedResponseText = "${command.level.userlevel} [username](tg://user?id=1) - 1";
        final String username = "username";
        BotRequest request = TestUtils.getRequestFromGroup("level " + username);
        Long chatId = request.getMessage().getChatId();

        when(userService.get(username)).thenReturn(TestUtils.getUser());

        BotResponse response = level.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(chatId);
    }

    @Test
    void parseWithNumberAsArgumentTest() {
        final String expectedResponseText = "saved";
        final int expectedChatLevel = 5;
        BotRequest request = TestUtils.getRequestFromGroup("level " + expectedChatLevel);
        Long chatId = request.getMessage().getChatId();

        when(chatService.get(chatId)).thenReturn(TestUtils.getChat(chatId));
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);

        BotResponse response = level.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        ArgumentCaptor<Chat> chatArgumentCaptor = ArgumentCaptor.forClass(Chat.class);
        verify(chatService).save(chatArgumentCaptor.capture());

        Chat savedChat = chatArgumentCaptor.getValue();
        assertEquals(chatId, savedChat.getChatId());
        assertEquals(expectedChatLevel, savedChat.getAccessLevel());

        verify(bot).sendTyping(chatId);
    }

    @Test
    void parseWithUsernameAndWrongLevelAsArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("level username test");

        assertThrows(BotException.class, () -> level.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithUnknownUsernameAndLevelAsArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("level username 5");

        assertThrows(BotException.class, () -> level.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithUsernameAndLevelAsArgumentTest() {
        final String expectedResponseText = "saved";
        final String username = "username";
        final int expectedUserLevel = 5;
        BotRequest request = TestUtils.getRequestFromGroup("level " + username + " " + expectedUserLevel);
        User requestdUser = TestUtils.getUser();

        when(userService.get(username)).thenReturn(requestdUser);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);

        BotResponse response = level.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userArgumentCaptor.capture());

        User savedUser = userArgumentCaptor.getValue();
        assertEquals(requestdUser.getUserId(), savedUser.getUserId());
        assertEquals(expectedUserLevel, savedUser.getAccessLevel());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithCommandAndLevelAsArgumentTest() {
        final String expectedResponseText = "saved";
        final String command = "commandName";
        final int expectedCommandLevel = 5;
        BotRequest request = TestUtils.getRequestFromGroup("level " + command + " " + expectedCommandLevel);

        CommandProperties commandProperties = new CommandProperties();
        when(commandPropertiesService.getCommand(command)).thenReturn(commandProperties);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);

        BotResponse response = level.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        ArgumentCaptor<CommandProperties> commandPropertiesArgumentCaptor = ArgumentCaptor.forClass(CommandProperties.class);
        verify(commandPropertiesService).save(commandPropertiesArgumentCaptor.capture());

        CommandProperties savedCommandProperties = commandPropertiesArgumentCaptor.getValue();
        assertEquals(commandProperties, savedCommandProperties);
        assertEquals(expectedCommandLevel, savedCommandProperties.getAccessLevel());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithCommandAsArgumentTest() {
        final String command = "commandName";
        final int expectedCommandLevel = 5;
        final String expectedResponseText = "${command.level.commandlevel} " + command + " - " + expectedCommandLevel;
        BotRequest request = TestUtils.getRequestFromGroup("level " + command);

        CommandProperties commandProperties = new CommandProperties().setCommandName(command).setAccessLevel(5);
        when(commandPropertiesService.getCommand(command)).thenReturn(commandProperties);

        BotResponse response = level.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

}