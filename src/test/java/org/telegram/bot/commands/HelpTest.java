package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.DisableCommand;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;

import java.util.Collections;
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
    private DisableCommandService disableCommandService;
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
        BotRequest request = TestUtils.getRequestFromGroup("help");

        when(propertiesConfig.getAdminId()).thenReturn(DEFAULT_USER_ID);
        when(userService.get(anyLong()))
                .thenReturn(new User().setUserId(DEFAULT_USER_ID).setAccessLevel(AccessLevel.NEWCOMER.getValue()));
        when(chatService.getChatAccessLevel(DEFAULT_CHAT_ID)).thenReturn(AccessLevel.NEWCOMER.getValue());
        when(userService.getUserAccessLevel(anyLong())).thenReturn(AccessLevel.ADMIN.getValue());
        when(disableCommandService.getByChat(request.getMessage().getChat())).thenReturn(Collections.emptyList());
        when(commandPropertiesService.getAvailableCommandsForLevel(anyInt()))
                .thenReturn(List.of(new CommandProperties().setClassName("className")));

        BotResponse botResponse = help.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(textResponse);

        String messageText = textResponse.getText();
        assertTrue(messageText.startsWith(grantingString));
        assertTrue(messageText.contains(currentLevelString + AccessLevel.ADMIN.getValue()));
    }

    @Test
    void getHelpFromChatTest() {
        final String expectedResponseText = """
                <b>${command.help.dontpanic}!</b>
                ${command.help.currentlevel} - <b>5</b> (${command.help.user} - 0; ${command.help.chat} - 5)
                ${command.help.countofdisabled}: 1
                ${command.help.listofavailablecommands} (1):
                /availableCommand — ${help.classname.name} (0)
                ${command.help.specificcommandhelp}
                """;
        BotRequest request = TestUtils.getRequestFromGroup("help");

        CommandProperties commandPropertiesOfAvailableCommand = new CommandProperties().setClassName("className").setId(1L).setCommandName("availableCommand").setAccessLevel(0);
        CommandProperties commandPropertiesOfDisabledCommand = new CommandProperties().setClassName("className").setId(2L);
        DisableCommand disabledCommand = new DisableCommand().setCommandProperties(commandPropertiesOfDisabledCommand);
        when(propertiesConfig.getAdminId()).thenReturn(ANOTHER_USER_ID);
        when(userService.get(anyLong()))
                .thenReturn(new User().setUserId(DEFAULT_USER_ID).setAccessLevel(AccessLevel.NEWCOMER.getValue()));
        when(chatService.getChatAccessLevel(DEFAULT_CHAT_ID)).thenReturn(AccessLevel.TRUSTED.getValue());
        when(disableCommandService.getByChat(request.getMessage().getChat())).thenReturn(List.of(disabledCommand));
        when(commandPropertiesService.getAvailableCommandsForLevel(anyInt()))
                .thenReturn(List.of(commandPropertiesOfAvailableCommand, commandPropertiesOfDisabledCommand));

        BotResponse botResponse = help.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(textResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void getHelpOfUnknownCommandTest() {
        BotRequest request = TestUtils.getRequestFromGroup("help abv");
        assertThrows(BotException.class, () -> help.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void getHelpOfCommandTest() {
        final String expectedResponseText = """
                <b>${command.help.commandinfo.name}:</b> ${help.help.name}
                <b>${command.help.commandinfo.desc}:</b> ${help.help.desc}
                <b>${command.help.commandinfo.args}:</b> ${help.help.params}
                <b>${command.help.commandinfo.examples}:</b> ${help.help.examples}
                <b>${command.help.commandinfo.comment}:</b> ${help.help.comment}
                <b>${command.help.commandinfo.level}:</b> 5""";
        BotRequest request = TestUtils.getRequestFromGroup("help abv");
        CommandProperties commandProperties = new CommandProperties()
                .setAccessLevel(AccessLevel.TRUSTED.getValue())
                .setClassName("help");

        when(commandPropertiesService.getCommand(anyString())).thenReturn(commandProperties);

        BotResponse botResponse = help.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TestUtils.checkDefaultTextResponseParams(textResponse);
        assertEquals(expectedResponseText, textResponse.getText());
    }
}