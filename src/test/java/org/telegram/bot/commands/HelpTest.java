package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.telegram.bot.TestUtils.*;

@ExtendWith(MockitoExtension.class)
class HelpTest {

    @Mock
    private Bot bot;
    @Mock
    private CommandPropertiesService commandPropertiesService;
    @Mock
    private UserService userService;
    @Mock
    private ChatService chatService;
    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private SpeechService speechService;

    @InjectMocks
    private Help help;

    @Test
    void firstGetHelpFromAdminTest() {
        final String currentLevelString = "${command.help.currentlevel} - <b>";
        final String grantingString = "${command.help.grants}";
        Update update = TestUtils.getUpdateFromGroup("help");

        when(propertiesConfig.getAdminId()).thenReturn(DEFAULT_USER_ID);
        when(userService.get(anyLong()))
                .thenReturn(new User().setUserId(DEFAULT_USER_ID).setAccessLevel(AccessLevel.NEWCOMER.getValue()));
        when(chatService.getChatAccessLevel(DEFAULT_CHAT_ID)).thenReturn(AccessLevel.NEWCOMER.getValue());
        when(userService.getUserAccessLevel(anyLong())).thenReturn(AccessLevel.ADMIN.getValue());
        when(commandPropertiesService.getAvailableCommandsForLevel(anyInt()))
                .thenReturn(List.of(new CommandProperties().setClassName("className")));

        SendMessage sendMessage = help.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage);

        String messageText = sendMessage.getText();
        assertTrue(messageText.startsWith(grantingString));
        assertTrue(messageText.contains(currentLevelString + AccessLevel.ADMIN.getValue()));
    }

    @Test
    void getHelpFromChatTest() {
        final String currentLevelString = "${command.help.currentlevel} - <b>";
        final String noPanic = "<b>${command.help.dontpanic}!</b>";
        Update update = TestUtils.getUpdateFromGroup("help");

        when(propertiesConfig.getAdminId()).thenReturn(ANOTHER_USER_ID);
        when(userService.get(anyLong()))
                .thenReturn(new User().setUserId(DEFAULT_USER_ID).setAccessLevel(AccessLevel.NEWCOMER.getValue()));
        when(chatService.getChatAccessLevel(DEFAULT_CHAT_ID)).thenReturn(AccessLevel.TRUSTED.getValue());

        SendMessage sendMessage = help.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage);

        String messageText = sendMessage.getText();
        assertTrue(messageText.startsWith(noPanic));
        assertTrue(messageText.contains(currentLevelString + AccessLevel.TRUSTED.getValue()));
    }

    @Test
    void getHelpOfUnknownCommandTest() {
        Update update = TestUtils.getUpdateFromGroup("help abv");
        assertThrows(BotException.class, () -> help.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void getHelpOfCommandTest() {
        final String expectedResponseText = "<b>${command.help.commandinfo.name}:</b> ${help.help.name}\n" +
                "<b>${command.help.commandinfo.desc}:</b> ${help.help.desc}\n" +
                "<b>${command.help.commandinfo.args}:</b> ${help.help.params}\n" +
                "<b>${command.help.commandinfo.examples}:</b> ${help.help.examples}\n" +
                "<b>${command.help.commandinfo.comment}:</b> ${help.help.comment}\n" +
                "<b>${command.help.commandinfo.level}:</b> 5";
        Update update = TestUtils.getUpdateFromGroup("help abv");
        CommandProperties commandProperties = new CommandProperties()
                .setAccessLevel(AccessLevel.TRUSTED.getValue())
                .setClassName("help")
                .setHelp(
                        new org.telegram.bot.domain.entities.Help()
                                .setName("Command")
                                .setDescription("Description")
                                .setParams("Params")
                                .setExamples("Examples"));

        when(commandPropertiesService.getCommand(anyString())).thenReturn(commandProperties);

        SendMessage sendMessage = help.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage);
        assertEquals(expectedResponseText, sendMessage.getText());
    }
}