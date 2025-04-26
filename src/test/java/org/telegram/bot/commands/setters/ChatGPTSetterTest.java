package org.telegram.bot.commands.setters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.TestUtils;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatGPTMessage;
import org.telegram.bot.domain.entities.ChatGPTSettings;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.ChatGPTRole;
import org.telegram.bot.services.ChatGPTMessageService;
import org.telegram.bot.services.ChatGPTSettingService;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.InternationalizationService;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatGPTSetterTest {

    @Mock
    private ChatGPTMessageService chatGPTMessageService;
    @Mock
    private ChatGPTSettingService chatGPTSettingService;
    @Mock
    private InternationalizationService internationalizationService;
    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private CommandWaitingService commandWaitingService;

    @InjectMocks
    private ChatGPTSetter chatGPTSetter;

    @BeforeEach
    void init() {
        when(internationalizationService.getAllTranslations("setter.chatgpt.emptycommand")).thenReturn(Set.of("chatgpt"));

        ReflectionTestUtils.invokeMethod(chatGPTSetter, "postConstruct");
    }

    @Test
    void canProcessedTest() {
        assertFalse(chatGPTSetter.canProcessed(""));
        assertFalse(chatGPTSetter.canProcessed(" "));
        assertFalse(chatGPTSetter.canProcessed("tratatam"));
        assertFalse(chatGPTSetter.canProcessed("chatgp"));
        assertTrue(chatGPTSetter.canProcessed("chatgpt"));
    }

    @Test
    void getAccessLevelTest() {
        assertEquals(AccessLevel.TRUSTED, chatGPTSetter.getAccessLevel());
    }

    @Test
    void setCallbackEmptyCommandFromGroupChatTest() {
        final String expectedResponseText = """
                ${setter.chatgpt.currentcontext}: <b>4 ${setter.chatgpt.messages}</b>
                Max: <b>16</b>
                
                """;
        final String argument = "chatgpt";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);
        Message message = request.getMessage();

        when(chatGPTMessageService.getMessages(message.getChat())).thenReturn(getSomeChatGPTMessages(message));
        when(propertiesConfig.getChatGPTContextSize()).thenReturn(16);

        BotResponse response = chatGPTSetter.set(request, argument);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);

        assertEquals(expectedResponseText, editResponse.getText());
        assertMainKeyboard(editResponse.getKeyboard());
    }

    @Test
    void setEmptyCommandFromPrivateChatTest() {
        final String expectedResponseText = """
                ${setter.chatgpt.currentcontext}: <b>4 ${setter.chatgpt.messages}</b>
                Max: <b>16</b>
                ${setter.chatgpt.currentprompt}: prompt
                """;
        final String argument = "chatgpt";
        BotRequest request = TestUtils.getRequestFromPrivate("set " + argument);
        Message message = request.getMessage();

        when(chatGPTMessageService.getMessages(message.getUser())).thenReturn(getSomeChatGPTMessages(message));
        when(propertiesConfig.getChatGPTContextSize()).thenReturn(16);
        when(propertiesConfig.getChatGPTModelsAvailable()).thenReturn(List.of("gpt-3.5-turbo", "gpt-4-turbo", "gpt-4o", "gpt-4o-mini", "o1-mini"));
        when(chatGPTSettingService.get(message.getChat())).thenReturn(new ChatGPTSettings().setPrompt("prompt").setModel("gpt-4o-mini"));

        BotResponse response = chatGPTSetter.set(request, argument);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());
        assertFullKeyboard(textResponse.getKeyboard());
    }

    @Test
    void setCallbackResetCacheGroupChatTest() {
        final String expectedResponseText = """
                ${setter.chatgpt.currentcontext}: <b>4 ${setter.chatgpt.messages}</b>
                Max: <b>16</b>
                
                """;
        final String argument = "chatgptrc";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);
        Message message = request.getMessage();

        when(chatGPTMessageService.getMessages(message.getChat())).thenReturn(getSomeChatGPTMessages(message));
        when(propertiesConfig.getChatGPTContextSize()).thenReturn(16);
        when(chatGPTSettingService.get(message.getChat())).thenReturn(new ChatGPTSettings());

        BotResponse response = chatGPTSetter.set(request, argument);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);

        assertEquals(expectedResponseText, editResponse.getText());
        assertMainKeyboard(editResponse.getKeyboard());
    }

    @Test
    void setCallbackResetCachePrivateChatTest() {
        final String expectedResponseText = """
                ${setter.chatgpt.currentcontext}: <b>4 ${setter.chatgpt.messages}</b>
                Max: <b>16</b>
                
                """;
        final String argument = "chatgptrc";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);
        Message message = request.getMessage();
        message.setChat(new Chat().setChatId(TestUtils.DEFAULT_USER_ID));

        when(chatGPTMessageService.getMessages(message.getUser())).thenReturn(getSomeChatGPTMessages(message));
        when(propertiesConfig.getChatGPTContextSize()).thenReturn(16);
        when(chatGPTSettingService.get(message.getChat())).thenReturn(new ChatGPTSettings());

        BotResponse response = chatGPTSetter.set(request, argument);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);

        assertEquals(expectedResponseText, editResponse.getText());
        assertMainKeyboard(editResponse.getKeyboard());
    }

    @Test
    void setResetCacheGroupChatTest() {
        final String expectedResponseText = """
                ${setter.chatgpt.currentcontext}: <b>4 ${setter.chatgpt.messages}</b>
                Max: <b>16</b>
                
                """;
        final String argument = "chatgptrc";
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);
        Message message = request.getMessage();

        when(chatGPTMessageService.getMessages(message.getChat())).thenReturn(getSomeChatGPTMessages(message));
        when(propertiesConfig.getChatGPTContextSize()).thenReturn(16);
        when(chatGPTSettingService.get(message.getChat())).thenReturn(new ChatGPTSettings());

        BotResponse response = chatGPTSetter.set(request, argument);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);

        assertEquals(expectedResponseText, editResponse.getText());
        assertMainKeyboard(editResponse.getKeyboard());
    }

    @Test
    void setResetCachePrivateChatTest() {
        final String expectedResponseText = """
                ${setter.chatgpt.currentcontext}: <b>4 ${setter.chatgpt.messages}</b>
                Max: <b>16</b>
                
                """;
        final String argument = "chatgptrc";
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);
        Message message = request.getMessage();
        message.setChat(new Chat().setChatId(TestUtils.DEFAULT_USER_ID));

        when(chatGPTMessageService.getMessages(message.getUser())).thenReturn(getSomeChatGPTMessages(message));
        when(propertiesConfig.getChatGPTContextSize()).thenReturn(16);
        when(chatGPTSettingService.get(message.getChat())).thenReturn(new ChatGPTSettings());

        BotResponse response = chatGPTSetter.set(request, argument);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);

        assertEquals(expectedResponseText, editResponse.getText());
        assertMainKeyboard(editResponse.getKeyboard());
    }

    @Test
    void setCallbackSelectModelFirstTimeTest() {
        final String expectedResponseText = """
                ${setter.chatgpt.currentcontext}: <b>4 ${setter.chatgpt.messages}</b>
                Max: <b>16</b>
                
                """;
        final String selectedModel = "testmodel";
        final String argument = "chatgptmd" + selectedModel;
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);
        Message message = request.getMessage();

        when(chatGPTMessageService.getMessages(message.getChat())).thenReturn(getSomeChatGPTMessages(message));
        when(propertiesConfig.getChatGPTContextSize()).thenReturn(16);

        BotResponse response = chatGPTSetter.set(request, argument);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);

        assertEquals(expectedResponseText, editResponse.getText());
        assertMainKeyboard(editResponse.getKeyboard());

        ArgumentCaptor<ChatGPTSettings> chatGPTSettingsCaptor = ArgumentCaptor.forClass(ChatGPTSettings.class);
        verify(chatGPTSettingService).save(chatGPTSettingsCaptor.capture());

        ChatGPTSettings chatGPTSettings = chatGPTSettingsCaptor.getValue();
        assertEquals(message.getChat(), chatGPTSettings.getChat());
        assertEquals(selectedModel, chatGPTSettings.getModel());
    }

    @Test
    void setCallbackSelectModelTest() {
        final String expectedResponseText = """
                ${setter.chatgpt.currentcontext}: <b>4 ${setter.chatgpt.messages}</b>
                Max: <b>16</b>
                ${setter.chatgpt.currentprompt}: prompt
                """;
        final String selectedModel = "gpt-4o-mini";
        final String argument = "chatgptmd" + selectedModel;
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);
        Message message = request.getMessage();
        Chat chat = message.getChat();

        when(chatGPTMessageService.getMessages(chat)).thenReturn(getSomeChatGPTMessages(message));
        when(propertiesConfig.getChatGPTContextSize()).thenReturn(16);
        when(propertiesConfig.getChatGPTModelsAvailable()).thenReturn(List.of("gpt-3.5-turbo", "gpt-4-turbo", "gpt-4o", "gpt-4o-mini", "o1-mini"));
        when(chatGPTSettingService.get(chat)).thenReturn(new ChatGPTSettings().setChat(chat).setPrompt("prompt").setModel("gpt-4o-mini"));

        BotResponse response = chatGPTSetter.set(request, argument);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);

        assertEquals(expectedResponseText, editResponse.getText());
        assertFullKeyboard(editResponse.getKeyboard());

        ArgumentCaptor<ChatGPTSettings> chatGPTSettingsCaptor = ArgumentCaptor.forClass(ChatGPTSettings.class);
        verify(chatGPTSettingService).save(chatGPTSettingsCaptor.capture());

        ChatGPTSettings chatGPTSettings = chatGPTSettingsCaptor.getValue();
        assertEquals(chat, chatGPTSettings.getChat());
        assertEquals(selectedModel, chatGPTSettings.getModel());
    }

    @Test
    void setCallbackSetPromptTest() {
        final String expectedResponseText = "${setter.chatgpt.setprompthelp}";
        final String argument = "chatgptpr";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);
        Message message = request.getMessage();

        BotResponse response = chatGPTSetter.set(request, argument);
        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);

        assertEquals(expectedResponseText, editResponse.getText());
        assertNull(editResponse.getKeyboard());

        verify(commandWaitingService).add(message.getChat(), message.getUser(), org.telegram.bot.commands.Set.class, "${setter.command} chatgptpr");
    }

    @Test
    void setSetPromptSettingsNotExistsTest() {
        final String expectedResponseText = """
                ${setter.chatgpt.currentcontext}: <b>4 ${setter.chatgpt.messages}</b>
                Max: <b>16</b>
                ${setter.chatgpt.currentprompt}: prompt
                """;
        final String prompt = "prompt";
        final String argument = "chatgptpr " + prompt;
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);
        Message message = request.getMessage();
        Chat chat = message.getChat();

        when(chatGPTMessageService.getMessages(chat)).thenReturn(getSomeChatGPTMessages(message));
        when(propertiesConfig.getChatGPTContextSize()).thenReturn(16);
        when(chatGPTSettingService.get(chat)).thenReturn(new ChatGPTSettings().setChat(chat).setPrompt(prompt).setModel("gpt-4o-mini"));

        BotResponse response = chatGPTSetter.set(request, argument);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());
        assertMainKeyboard(textResponse.getKeyboard());

        assertEquals(expectedResponseText, textResponse.getText());
        assertMainKeyboard(textResponse.getKeyboard());

        verify(commandWaitingService).remove(chat, message.getUser());

        ArgumentCaptor<ChatGPTSettings> chatGPTSettingsCaptor = ArgumentCaptor.forClass(ChatGPTSettings.class);
        verify(chatGPTSettingService).save(chatGPTSettingsCaptor.capture());

        ChatGPTSettings chatGPTSettings = chatGPTSettingsCaptor.getValue();
        assertEquals(chat, chatGPTSettings.getChat());
        assertEquals(prompt, chatGPTSettings.getPrompt());
    }

    @Test
    void setSetPromptSettingsTest() {
        final String expectedResponseText = """
                ${setter.chatgpt.currentcontext}: <b>4 ${setter.chatgpt.messages}</b>
                Max: <b>16</b>
                
                """;
        final String prompt = "prompt";
        final String argument = "chatgptpr " + prompt;
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);
        Message message = request.getMessage();
        Chat chat = message.getChat();

        when(chatGPTMessageService.getMessages(chat)).thenReturn(getSomeChatGPTMessages(message));
        when(propertiesConfig.getChatGPTContextSize()).thenReturn(16);

        BotResponse response = chatGPTSetter.set(request, argument);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());
        assertMainKeyboard(textResponse.getKeyboard());

        assertEquals(expectedResponseText, textResponse.getText());
        assertMainKeyboard(textResponse.getKeyboard());

        verify(commandWaitingService).remove(chat, message.getUser());

        ArgumentCaptor<ChatGPTSettings> chatGPTSettingsCaptor = ArgumentCaptor.forClass(ChatGPTSettings.class);
        verify(chatGPTSettingService).save(chatGPTSettingsCaptor.capture());

        ChatGPTSettings chatGPTSettings = chatGPTSettingsCaptor.getValue();
        assertEquals(chat, chatGPTSettings.getChat());
        assertEquals(prompt, chatGPTSettings.getPrompt());
    }

    private void assertFullKeyboard(Keyboard keyboard) {
        List<List<KeyboardButton>> keyboardButtonsList = keyboard.getKeyboardButtonsList();
        assertEquals(8, keyboardButtonsList.size());

        List<KeyboardButton> selectModelRow1 = keyboardButtonsList.get(0);
        assertEquals(1, selectModelRow1.size());
        KeyboardButton selectModelButton1 = selectModelRow1.get(0);
        assertEquals("gpt-3.5-turbo", selectModelButton1.getName());
        assertEquals("${setter.command} chatgptmdgpt-3.5-turbo", selectModelButton1.getCallback());

        List<KeyboardButton> selectModelRow2 = keyboardButtonsList.get(1);
        assertEquals(1, selectModelRow2.size());
        KeyboardButton selectModelButton2 = selectModelRow2.get(0);
        assertEquals("gpt-4-turbo", selectModelButton2.getName());
        assertEquals("${setter.command} chatgptmdgpt-4-turbo", selectModelButton2.getCallback());

        List<KeyboardButton> selectModelRow3 = keyboardButtonsList.get(2);
        assertEquals(1, selectModelRow3.size());
        KeyboardButton selectModelButton3 = selectModelRow3.get(0);
        assertEquals("gpt-4o", selectModelButton3.getName());
        assertEquals("${setter.command} chatgptmdgpt-4o", selectModelButton3.getCallback());

        List<KeyboardButton> selectModelRow4 = keyboardButtonsList.get(3);
        assertEquals(1, selectModelRow4.size());
        KeyboardButton selectModelButton4 = selectModelRow4.get(0);
        assertEquals("✔\uFE0Fgpt-4o-mini", selectModelButton4.getName());
        assertEquals("${setter.command} chatgptmdgpt-4o-mini", selectModelButton4.getCallback());

        List<KeyboardButton> selectModelRow5 = keyboardButtonsList.get(4);
        assertEquals(1, selectModelRow5.size());
        KeyboardButton selectModelButton5 = selectModelRow5.get(0);
        assertEquals("o1-mini", selectModelButton5.getName());
        assertEquals("${setter.command} chatgptmdo1-mini", selectModelButton5.getCallback());

        assertMainKeyboard(keyboardButtonsList.get(5), keyboardButtonsList.get(6), keyboardButtonsList.get(7));
    }

    private void assertMainKeyboard(Keyboard keyboard) {
        List<List<KeyboardButton>> keyboardButtonsList = keyboard.getKeyboardButtonsList();
        assertEquals(3, keyboardButtonsList.size());
        assertMainKeyboard(keyboardButtonsList.get(0), keyboardButtonsList.get(1), keyboardButtonsList.get(2));
    }

    private void assertMainKeyboard(List<KeyboardButton> setPromptRow, List<KeyboardButton> resetCacheRow, List<KeyboardButton> backRow) {
        assertEquals(1, setPromptRow.size());
        KeyboardButton setPromptButton = setPromptRow.get(0);
        assertEquals("⚙\uFE0F${setter.chatgpt.button.setprompt}", setPromptButton.getName());
        assertEquals("${setter.command} chatgptpr", setPromptButton.getCallback());

        assertEquals(1, resetCacheRow.size());
        KeyboardButton setCacheButton = resetCacheRow.get(0);
        assertEquals("\uD83D\uDDD1\uFE0F${setter.chatgpt.button.resetcache}", setCacheButton.getName());
        assertEquals("${setter.command} chatgptrc", setCacheButton.getCallback());

        assertEquals(1, backRow.size());
        KeyboardButton backButton = backRow.get(0);
        assertEquals("↩\uFE0F${setter.chatgpt.button.settings}", backButton.getName());
        assertEquals("${setter.command} back", backButton.getCallback());
    }

    private List<ChatGPTMessage> getSomeChatGPTMessages(Message message) {
        Chat chat = message.getChat();
        User user = message.getUser();

        return List.of(
                new ChatGPTMessage().setId(1L).setChat(chat).setUser(user).setRole(ChatGPTRole.USER).setContent("usermessage1"),
                new ChatGPTMessage().setId(2L).setChat(chat).setUser(user).setRole(ChatGPTRole.ASSISTANT).setContent("assistantmessage1"),
                new ChatGPTMessage().setId(3L).setChat(chat).setUser(user).setRole(ChatGPTRole.USER).setContent("usermessage2"),
                new ChatGPTMessage().setId(4L).setChat(chat).setUser(user).setRole(ChatGPTRole.ASSISTANT).setContent("assistantmessage1")
        );
    }

}