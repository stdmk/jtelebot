package org.telegram.bot.commands.setters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Alias;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.EditResponse;
import org.telegram.bot.domain.model.response.KeyboardButton;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.AliasService;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AliasSetterTest {

    @Mock
    private AliasService aliasService;
    @Mock
    private SpeechService speechService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private InternationalizationService internationalizationService;

    @InjectMocks
    private AliasSetter aliasSetter;

    @BeforeEach
    void init() {
        when(internationalizationService.getAllTranslations("setter.alias.emptycommand")).thenReturn(Set.of("alias"));
        when(internationalizationService.internationalize("${setter.alias.emptycommand} ${setter.alias.update}")).thenReturn(Set.of("alias update"));
        when(internationalizationService.internationalize("${setter.alias.emptycommand} ${setter.alias.remove}")).thenReturn(Set.of("alias delete"));
        when(internationalizationService.internationalize("${setter.alias.emptycommand} ${setter.alias.add}")).thenReturn(Set.of("alias add"));
        when(internationalizationService.internationalize("${setter.alias.emptycommand} ${setter.alias.select}")).thenReturn(Set.of("alias select"));

        ReflectionTestUtils.invokeMethod(aliasSetter, "postConstruct");
    }

    @Test
    void canProcessedTest() {
        assertFalse(aliasSetter.canProcessed(""));
        assertFalse(aliasSetter.canProcessed(" "));
        assertFalse(aliasSetter.canProcessed("tratatam"));
        assertFalse(aliasSetter.canProcessed("alia"));
        assertTrue(aliasSetter.canProcessed("alias"));
    }

    @Test
    void getAccessLevelTest() {
        assertEquals(AccessLevel.TRUSTED, aliasSetter.getAccessLevel());
    }

    @Test
    void setWithUnknownCommandTest() {
        final String expectedErrorText = "error";
        final String argument = "test";
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> aliasSetter.set(request, argument));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"alias", "alias update"})
    void setCallbackWithoutArgumentsTest(String argument) {
        final String expectedResponseText = """
                <b>${setter.alias.listcaption}:</b>
                1. test1
                2. test1
                3. test2
                """;
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);
        Message message = request.getMessage();

        Page<Alias> aliasEntityList = getSomeAliasEntityList(message);
        when(aliasService.getByChatAndUser(message.getChat(), message.getUser(), 0)).thenReturn(aliasEntityList);

        BotResponse response = aliasSetter.set(request, argument);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);
        assertEquals(expectedResponseText, editResponse.getText());

        assertAliasListKeyboard(editResponse.getKeyboard().getKeyboardButtonsList(), aliasEntityList);
    }

    @Test
    void setCallbackWithDeleteAliasSelectPageAsArgumentTest() {
        final String expectedResponseText = """
                <b>${setter.alias.listcaption}:</b>
                1. test1
                2. test1
                3. test2
                """;
        final String argument = "alias delete ";
        final int page = 1;
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument + page);
        Message message = request.getMessage();

        when(internationalizationService.internationalize("${setter.alias.emptycommand} ${setter.alias.remove} ${setter.alias.selectpage}"))
                .thenReturn(Set.of(argument));
        Page<Alias> aliasEntityList = getSomeAliasEntityList(message);
        when(aliasService.getByChatAndUser(message.getChat(), message.getUser(), 1)).thenReturn(aliasEntityList);

        BotResponse response = aliasSetter.set(request, argument + page);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);
        assertEquals(expectedResponseText, editResponse.getText());

        assertAliasListKeyboard(editResponse.getKeyboard().getKeyboardButtonsList(), aliasEntityList, page);
    }

    @Test
    void setCallbackWithDeleteAliasAsArgumentTest() {
        final String expectedResponseText = """
                <b>${setter.alias.listcaption}:</b>
                1. test1
                2. test1
                3. test2
                """;
        final String argument = "alias delete 1";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);
        Message message = request.getMessage();

        Page<Alias> aliasEntityList = getSomeAliasEntityList(message);
        when(aliasService.getByChatAndUser(message.getChat(), message.getUser(), 0)).thenReturn(aliasEntityList);

        BotResponse response = aliasSetter.set(request, argument);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);
        assertEquals(expectedResponseText, editResponse.getText());

        assertAliasListKeyboard(editResponse.getKeyboard().getKeyboardButtonsList(), aliasEntityList);
    }

    @Test
    void setCallbackWithAddAliasAsArgumentTest() {
        final String expectedResponseText = """
                <b>${setter.alias.listcaption}:</b>
                1. test1
                2. test1
                3. test2
                
                ${setter.alias.commandwaitingstart}""";
        final String argument = "alias add";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);
        Message message = request.getMessage();

        Page<Alias> aliasEntityList = getSomeAliasEntityList(message);
        when(aliasService.getByChatAndUser(message.getChat(), message.getUser(), 0)).thenReturn(aliasEntityList);

        BotResponse response = aliasSetter.set(request, argument);

        verify(commandWaitingService).add(message.getChat(), message.getUser(), org.telegram.bot.commands.Set.class, "${setter.command} ${setter.alias.emptycommand} ${setter.alias.add}");

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);
        assertEquals(expectedResponseText, editResponse.getText());

        assertAliasListKeyboard(editResponse.getKeyboard().getKeyboardButtonsList(), aliasEntityList);
    }

    @Test
    void setCallbackWithSelectAliasSelectPageAsArgumentTest() {
        final String expectedResponseText = "<b>${setter.alias.chatlistcaption}:</b>";
        final String argument = "alias select ";
        final int page = 1;
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument + page);
        Message message = request.getMessage();

        when(internationalizationService.internationalize("${setter.alias.emptycommand} ${setter.alias.select} ${setter.alias.selectpage}"))
                .thenReturn(Set.of(argument));
        Page<Alias> aliasEntityList = getSomeAliasEntityList(message);
        when(aliasService.getByChat(message.getChat(), 1)).thenReturn(aliasEntityList);
        when(aliasService.getByChatAndUser(message.getChat(), message.getUser())).thenReturn(aliasEntityList.stream().toList());

        BotResponse response = aliasSetter.set(request, argument + page);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);
        assertEquals(expectedResponseText, editResponse.getText());

        assertAliasListKeyboard(editResponse.getKeyboard().getKeyboardButtonsList(), aliasEntityList, page, false);
    }

    @Test
    void setCallbackWithSelectAlreadyExistenceAliasTest() {
        final String expectedErrorText = "";
        final Long aliasId = 1L;
        final String argument = "alias select " + aliasId;
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);
        Message message = request.getMessage();

        Alias aliasEntity = getSomeAliasEntityList(message).iterator().next();
        when(aliasService.get(aliasId)).thenReturn(aliasEntity);
        when(aliasService.get(message.getChat(), message.getUser(), aliasEntity.getName())).thenReturn(aliasEntity);
        when(speechService.getRandomMessageByTag(BotSpeechTag.DUPLICATE_ENTRY)).thenReturn(expectedErrorText);

        BotResponse response = aliasSetter.set(request, argument);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedErrorText, textResponse.getText());
    }

    @Test
    void setCallbackWithSelectAliasAsArgumentTest() {
        final String expectedResponseText = """
                <b>${setter.alias.listcaption}:</b>
                1. test1
                2. test1
                3. test2
                """;
        final Long aliasId = 1L;
        final String argument = "alias select " + aliasId;
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        Page<Alias> aliasEntityList = getSomeAliasEntityList(message);
        Alias selectedAlias = aliasEntityList.iterator().next();
        when(aliasService.get(aliasId)).thenReturn(selectedAlias);
        when(aliasService.getByChatAndUser(chat, user, 0)).thenReturn(aliasEntityList);

        BotResponse response = aliasSetter.set(request, argument);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);
        assertEquals(expectedResponseText, editResponse.getText());

        ArgumentCaptor<Alias> aliasCaptor = ArgumentCaptor.forClass(Alias.class);
        verify(aliasService).save(aliasCaptor.capture());
        Alias savedAlias = aliasCaptor.getValue();
        assertEquals(chat, savedAlias.getChat());
        assertEquals(user, savedAlias.getUser());
        assertEquals(selectedAlias.getName(), savedAlias.getName());
        assertEquals(selectedAlias.getValue(), savedAlias.getValue());

        assertAliasListKeyboard(editResponse.getKeyboard().getKeyboardButtonsList(), aliasEntityList);
    }

    @Test
    void setWithEmptyCommandAsArgumentTest() {
        final String expectedResponseText = """
                <b>${setter.alias.listcaption}:</b>
                1. test1
                2. test1
                3. test2
                """;
        final String argument = "alias";
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);
        Message message = request.getMessage();
        Page<Alias> aliasEntityList = getSomeAliasEntityList(message);

        when(aliasService.getByChatAndUser(message.getChat(), message.getUser(), 0)).thenReturn(aliasEntityList);

        BotResponse response = aliasSetter.set(request, argument);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());

        assertAliasListKeyboard(textResponse.getKeyboard().getKeyboardButtonsList(), aliasEntityList);
    }

    @Test
    void setWithWrongDeleteCommandAsArgumentTest() {
        final String expectedErrorText = "error";
        final String argument = "alias delete";
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> aliasSetter.set(request, argument));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void setWithDeleteCommandWrongAliasIdAsArgumentTest() {
        final String expectedResponseText = "error";
        final String argument = "alias delete a";
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedResponseText);

        BotResponse response = aliasSetter.set(request, argument);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void setWithDeleteUnknownAliasByAliasIdAsArgumentTest() {
        final String expectedResponseText = "error";
        final long aliasId = 1L;
        final String argument = "alias delete " + aliasId;
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedResponseText);

        BotResponse response = aliasSetter.set(request, argument);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void setWithDeleteByAliasIdAsArgumentTest() {
        final String expectedResponseText = "saved";
        final long aliasId = 1L;
        final String argument = "alias delete " + aliasId;
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);
        Message message = request.getMessage();

        when(aliasService.remove(message.getChat(), message.getUser(), aliasId)).thenReturn(true);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);

        BotResponse response = aliasSetter.set(request, argument);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void setWithDeleteUnknownAliasByAliasNameAsArgumentTest() {
        final String expectedResponseText = "error";
        final String aliasName = "name";
        final String argument = "alias delete " + aliasName;
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedResponseText);

        BotResponse response = aliasSetter.set(request, argument);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void setWithDeleteByAliasNameAsArgumentTest() {
        final String expectedResponseText = "saved";
        final String aliasName = "name";
        final String argument = "alias delete " + aliasName;
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);
        Message message = request.getMessage();

        when(aliasService.remove(message.getChat(), message.getUser(), aliasName)).thenReturn(true);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);

        BotResponse response = aliasSetter.set(request, argument);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void setWithEmptyAddAliasCommandAsArgumentTest() {
        final String expectedResponseText = """
                <b>${setter.alias.listcaption}:</b>
                1. test1
                2. test1
                3. test2
                
                ${setter.alias.commandwaitingstart}""";
        final String argument = "alias add";
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);
        Message message = request.getMessage();
        Page<Alias> aliasEntityList = getSomeAliasEntityList(message);

        when(aliasService.getByChatAndUser(message.getChat(), message.getUser(), 0)).thenReturn(aliasEntityList);

        BotResponse response = aliasSetter.set(request, argument);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());

        assertAliasListKeyboard(textResponse.getKeyboard().getKeyboardButtonsList(), aliasEntityList);
    }

    @Test
    void setWithWrongAddAliasCommandAsArgumentTest() {
        final String expectedResponseText = "error";
        final String aliasName = "name";
        final String argument = "alias add " + aliasName;
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);
        Message message = request.getMessage();

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedResponseText);

        BotResponse response = aliasSetter.set(request, argument);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());

        verify(commandWaitingService).remove(message.getChat(), message.getUser());
    }

    @Test
    void setWithAddAliasCommandWithTooMuchAliasesAsArgumentTest() {
        final String expectedErrorText = "error";
        final String aliases = "{name value;name value;name value;name value;name value;name value;}";
        final String argument = "alias add name " + aliases;
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> aliasSetter.set(request, argument));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void setWithAddAliasCommandAsArgumentWithAlreadyExistenceAliasTest() {
        final String expectedResponseText = "error";
        final String aliasName = "name";
        final String aliases = "{name value;name value;name value;name value;name value;}";
        final String argument = "alias add " + aliasName + " " + aliases;
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);
        Message message = request.getMessage();

        when(aliasService.get(message.getChat(), message.getUser(), aliasName)).thenReturn(new Alias());
        when(speechService.getRandomMessageByTag(BotSpeechTag.DUPLICATE_ENTRY)).thenReturn(expectedResponseText);

        BotResponse response = aliasSetter.set(request, argument);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void setWithAddAliasCommandAsArgumentTest() {
        final String expectedResponseText = "saved";
        final String aliasName = "name";
        final String argument = "alias add " + aliasName + " value";
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);
        Message message = request.getMessage();

        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);

        BotResponse response = aliasSetter.set(request, argument);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);

        assertEquals(expectedResponseText, textResponse.getText());

        verify(commandWaitingService).remove(message.getChat(), message.getUser());
    }

    private void assertAliasListKeyboard(List<List<KeyboardButton>> keyboardButtonsList, Page<Alias> aliasEntityList) {
        assertAliasListKeyboard(keyboardButtonsList, aliasEntityList, 0, true);
    }

    private void assertAliasListKeyboard(List<List<KeyboardButton>> keyboardButtonsList, Page<Alias> aliasEntityList, int page) {
        assertAliasListKeyboard(keyboardButtonsList, aliasEntityList, page, true);
    }

    private void assertAliasListKeyboard(List<List<KeyboardButton>> keyboardButtonsList, Page<Alias> aliasEntityList, Integer page, boolean deleteEmoji) {
        assertEquals(8, keyboardButtonsList.size());

        String command;
        if (deleteEmoji) {
            command = "remove";
        } else {
            command = "select";
        }

        int i = 0;
        for (Alias alias : aliasEntityList) {
            KeyboardButton keyboardButton = keyboardButtonsList.get(i).get(0);
            String buttonName;
            if (deleteEmoji) {
                buttonName = "❌" + alias.getName() + " — " + alias.getValue();
            } else {
                buttonName = alias.getName() + " — " + alias.getValue();
            }
            assertEquals(buttonName, keyboardButton.getName());
            assertEquals("${setter.command} ${setter.alias.emptycommand} ${setter.alias." + command + "} " + alias.getId(), keyboardButton.getCallback());
            i = i + 1;
        }

        List<KeyboardButton> selectPageList = keyboardButtonsList.get(3);
        if (page > 0) {
            assertEquals(2, selectPageList.size());
            KeyboardButton backButton = selectPageList.get(0);
            assertEquals("⬅\uFE0F${setter.alias.button.back}", backButton.getName());
            assertEquals("${setter.command} ${setter.alias.emptycommand} ${setter.alias." + command + "} ${setter.alias.selectpage}0", backButton.getCallback());
            KeyboardButton forwardButton = selectPageList.get(1);
            assertEquals("${setter.alias.button.forward}➡\uFE0F", forwardButton.getName());
            assertEquals("${setter.command} ${setter.alias.emptycommand} ${setter.alias." + command + "} ${setter.alias.selectpage}2", forwardButton.getCallback());
        } else {
            assertEquals(1, selectPageList.size());
            KeyboardButton forwardButton = selectPageList.get(0);
            assertEquals("${setter.alias.button.forward}➡\uFE0F", forwardButton.getName());
            assertEquals("${setter.command} ${setter.alias.emptycommand} ${setter.alias." + command + "} ${setter.alias.selectpage}1", forwardButton.getCallback());
        }

        assertMainButtons(keyboardButtonsList.get(4).get(0), keyboardButtonsList.get(5).get(0), keyboardButtonsList.get(6).get(0), keyboardButtonsList.get(7).get(0));
    }

    private void assertMainButtons(KeyboardButton addButton, KeyboardButton selectButton, KeyboardButton updateButton, KeyboardButton backButton) {
        assertEquals("\uD83C\uDD95${setter.alias.button.add}", addButton.getName());
        assertEquals("${setter.command} ${setter.alias.emptycommand} ${setter.alias.add}", addButton.getCallback());

        assertEquals("⤴\uFE0F${setter.alias.button.select}", selectButton.getName());
        assertEquals("${setter.command} ${setter.alias.emptycommand} ${setter.alias.select} ${setter.alias.selectpage}0", selectButton.getCallback());

        assertEquals("\uD83D\uDD04${setter.alias.button.update}", updateButton.getName());
        assertEquals("${setter.command} ${setter.alias.emptycommand} ${setter.alias.update}", updateButton.getCallback());

        assertEquals("↩\uFE0F${setter.alias.button.settings}", backButton.getName());
        assertEquals("${setter.command} back", backButton.getCallback());
    }

    private Page<Alias> getSomeAliasEntityList(Message message) {
        Chat chat = message.getChat();
        User user = message.getUser();

        List<Alias> aliases = List.of(
                new Alias()
                        .setId(1L)
                        .setChat(chat)
                        .setUser(user)
                        .setName("test1")
                        .setValue("echo1"),
                new Alias()
                        .setId(2L)
                        .setChat(chat)
                        .setUser(user)
                        .setName("test1")
                        .setValue("echo1"),
                new Alias()
                        .setId(3L)
                        .setChat(chat)
                        .setUser(user)
                        .setName("test2")
                        .setValue("echo2"));

        return new PageImpl<>(aliases, Pageable.ofSize(100), 1000);
    }

}