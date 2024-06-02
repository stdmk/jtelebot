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
import org.telegram.bot.domain.entities.TodoTag;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.TodoService;
import org.telegram.bot.services.TodoTagService;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.telegram.bot.TestUtils.DEFAULT_CHAT_ID;

@ExtendWith(MockitoExtension.class)
class TodoTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;
    @Mock
    private TodoService todoService;
    @Mock
    private TodoTagService todoTagService;

    @InjectMocks
    private Todo todo;

    @Test
    void parseWithoutArgumentsTest() {
        final String expectedResponse = "<b>${command.todo.alllistcaption}:</b>\n" +
                "description1\n" +
                "#tag1 #tag #tag3 \n" +
                "/todo_delnull\n" +
                "\n" +
                "description2\n" +
                "#tag4 #tag #tag5 \n" +
                "/todo_delnull\n" +
                "\n" +
                "description3\n" +
                "#tag6 #tag #tag7 \n" +
                "/todo_delnull\n";
        BotRequest request = TestUtils.getRequestFromGroup("trigger");

        when(todoService.get(any(Chat.class), any(User.class))).thenReturn(getSomeTodos());

        BotResponse botResponse = todo.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponse, textResponse.getText());
        verify(bot).sendTyping(DEFAULT_CHAT_ID);
    }

    @Test
    void parseDelArgumentNotIntTest() {
        BotRequest request = TestUtils.getRequestFromGroup("trigger_dela");
        assertThrows(BotException.class, () -> todo.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(DEFAULT_CHAT_ID);
    }

    @Test
    void parseDelArgumentWithNotExistenceTriggerTest() {
        BotRequest request = TestUtils.getRequestFromGroup("trigger_del123");

        when(todoService.get(any(Chat.class), anyLong())).thenReturn(null);

        assertThrows(BotException.class, () -> todo.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendTyping(DEFAULT_CHAT_ID);
    }

    @Test
    void parseDelArgumentWithNotOwnerTriggerTest() {
        BotRequest request = TestUtils.getRequestFromGroup("trigger_del123");

        when(todoService.get(any(Chat.class), anyLong())).thenReturn(
                new org.telegram.bot.domain.entities.Todo()
                        .setUser(TestUtils.getUser(TestUtils.ANOTHER_USER_ID)));

        assertThrows(BotException.class, () -> todo.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NOT_OWNER);
        verify(bot).sendTyping(DEFAULT_CHAT_ID);
    }

    @Test
    void parseDelArgumentTest() {
        final String expectedResponseText = "saved";
        BotRequest request = TestUtils.getRequestFromGroup("trigger_del123");

        when(todoService.get(any(Chat.class), anyLong())).thenReturn(
                new org.telegram.bot.domain.entities.Todo()
                        .setUser(TestUtils.getUser(TestUtils.DEFAULT_USER_ID)));
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);

        BotResponse botResponse = todo.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(todoService).remove(123L);
        verify(bot).sendTyping(DEFAULT_CHAT_ID);
    }

    @Test
    void parseWithTextWithoutTagsTest() {
        final String expectedResponseText = "saved";
        final String expectedTodoText = "test";
        BotRequest request = TestUtils.getRequestFromGroup("trigger " + expectedTodoText);

        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);

        BotResponse botResponse = todo.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        ArgumentCaptor<org.telegram.bot.domain.entities.Todo> todoCaptor = ArgumentCaptor.forClass(org.telegram.bot.domain.entities.Todo.class);
        verify(todoService).save(todoCaptor.capture());
        org.telegram.bot.domain.entities.Todo todo = todoCaptor.getValue();

        assertEquals(TestUtils.getChat().getChatId(), todo.getChat().getChatId());
        assertEquals(TestUtils.getUser().getUserId(), todo.getUser().getUserId());
        assertTrue(todo.getTags().isEmpty());
        assertEquals(expectedTodoText, todo.getTodoText());
        verify(bot).sendTyping(DEFAULT_CHAT_ID);
    }

    @Test
    void parseWithTextWithTagsTest() {
        String tag1 = "tag1";
        String tag2 = "tag2";
        List<String> tagList = List.of(tag1, tag2);
        final String expectedResponseText = "saved";
        final String expectedTodoText = "test";
        BotRequest request = TestUtils.getRequestFromGroup("trigger " + expectedTodoText + " #" + tag1 + " #" + tag2);

        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);

        BotResponse botResponse = todo.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        ArgumentCaptor<org.telegram.bot.domain.entities.Todo> todoCaptor = ArgumentCaptor.forClass(org.telegram.bot.domain.entities.Todo.class);
        verify(todoService).save(todoCaptor.capture());
        org.telegram.bot.domain.entities.Todo todo = todoCaptor.getValue();

        assertEquals(TestUtils.getChat().getChatId(), todo.getChat().getChatId());
        assertEquals(TestUtils.getUser().getUserId(), todo.getUser().getUserId());
        assertEquals(expectedTodoText, todo.getTodoText());

        List<TodoTag> tags = todo.getTags();
        assertEquals(tagList.size(), tags.size());
        tags.forEach(tag -> {
            assertEquals(TestUtils.getChat().getChatId(), tag.getChat().getChatId());
            assertEquals(TestUtils.getUser().getUserId(), tag.getUser().getUserId());
            assertEquals(todo, tag.getTodo());
        });
        assertEquals(0, tags.stream().map(TodoTag::getTag).filter(tag -> !tagList.contains(tag)).count());
        verify(bot).sendTyping(DEFAULT_CHAT_ID);
    }

    @Test
    void parseWithoutTextWithTagsTest() {
        final String expectedResponseText = "<b>${command.todo.foundlistcaption}:</b>\n" +
                "description1\n" +
                "#tag1 #tag #tag3 \n" +
                "/todo_delnull\n" +
                "\n" +
                "description2\n" +
                "#tag4 #tag #tag5 \n" +
                "/todo_delnull\n" +
                "\n" +
                "description3\n" +
                "#tag6 #tag #tag7 \n" +
                "/todo_delnull\n";
        String tag1 = "tag1";
        String tag2 = "tag2";
        List<TodoTag> todoTagsList = getSomeTodos()
                .stream()
                .map(org.telegram.bot.domain.entities.Todo::getTags)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        BotRequest request = TestUtils.getRequestFromGroup("trigger #" + tag1 + " #" + tag2);

        when(todoTagService.get(any(Chat.class), any(User.class), anyList())).thenReturn(todoTagsList);

        BotResponse botResponse = todo.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
        verify(bot).sendTyping(DEFAULT_CHAT_ID);
    }

    private List<org.telegram.bot.domain.entities.Todo> getSomeTodos() {
        org.telegram.bot.domain.entities.Todo trigger1 = new org.telegram.bot.domain.entities.Todo()
                .setTodoText("description1");
        org.telegram.bot.domain.entities.Todo trigger2 = new org.telegram.bot.domain.entities.Todo()
                .setTodoText("description2");
        org.telegram.bot.domain.entities.Todo trigger3 = new org.telegram.bot.domain.entities.Todo()
                .setTodoText("description3");

        trigger1.setTags(List.of(
                new TodoTag().setTag("tag1").setTodo(trigger1),
                new TodoTag().setTag("tag").setTodo(trigger1),
                new TodoTag().setTag("tag3").setTodo(trigger1)));
        trigger2.setTags(List.of(
                new TodoTag().setTag("tag4").setTodo(trigger2),
                new TodoTag().setTag("tag").setTodo(trigger2),
                new TodoTag().setTag("tag5").setTodo(trigger2)));
        trigger3.setTags(List.of(
                new TodoTag().setTag("tag6").setTodo(trigger3),
                new TodoTag().setTag("tag").setTodo(trigger3),
                new TodoTag().setTag("tag7").setTodo(trigger3)));

        return new LinkedList<>(List.of(trigger1, trigger2, trigger3));
    }

}