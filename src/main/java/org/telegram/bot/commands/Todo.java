package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.TodoTag;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.TodoService;
import org.telegram.bot.services.TodoTagService;
import org.telegram.bot.utils.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Todo implements Command {

    private static final Pattern TAGS_PATTERN = Pattern.compile("(^|\\B)#(?![0-9_]+\\b)([a-zA-Zа-яА-Я0-9_]{1,30})(\\b|\\r)");
    private static final String TAG_SYMBOL = "#";

    private final Bot bot;
    private final SpeechService speechService;
    private final TodoService todoService;
    private final TodoTagService todoTagService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Long chatId = message.getChatId();

        bot.sendTyping(chatId);

        Chat chat = message.getChat();
        User user = message.getUser();
        String responseText;

        String commandArgument = message.getCommandArgument();
        if (commandArgument == null) {
            responseText = getTodoList(chat, user);
        } else {
            if (commandArgument.startsWith("_del")) {
                removeTodo(chat, user, commandArgument.substring(4));
                responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
            } else {
                List<String> tags = getTags(commandArgument);
                String todoText = getDescription(tags, commandArgument);

                if (!StringUtils.hasText(todoText)) {
                    responseText = searchTodosByTags(chat, user, tags);
                } else {
                    addNewTodo(chat, user, tags, todoText);
                    responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
                }
            }
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(new ResponseSettings()
                        .setFormattingStyle(FormattingStyle.HTML)
                        .setWebPagePreview(false)));
    }

    private String getTodoList(Chat chat, User user) {
        return "<b>${command.todo.alllistcaption}:</b>\n" + todoListToString(todoService.get(chat, user));
    }

    private void removeTodo(Chat chat, User user, String id) {
        long todoId;
        try {
            todoId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        org.telegram.bot.domain.entities.Todo todo = todoService.get(chat, todoId);
        if (todo == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (!todo.getUser().getUserId().equals(user.getUserId())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER));
        }

        todoService.remove(todoId);
    }

    private List<String> getTags(String text) {
        ArrayList<String> tags = new ArrayList<>();

        Matcher matcher = TAGS_PATTERN.matcher(text);
        while (matcher.find()) {
            tags.add(matcher.group(2));
        }

        return tags;
    }

    private String getDescription(List<String> tags, String text) {
        String textWithoutTags = text;
        for (String tag : tags) {
            textWithoutTags = textWithoutTags.replace(TAG_SYMBOL + tag, "");
        }

        return TextUtils.reduceSpaces(textWithoutTags);
    }

    private String searchTodosByTags(Chat chat, User user, List<String> tags) {
        List<org.telegram.bot.domain.entities.Todo> deduplicatedTodoList = new ArrayList<>();

        todoTagService.get(chat, user, tags)
                .stream()
                .map(TodoTag::getTodo)
                .forEach(todo -> {
                    if (!deduplicatedTodoList.contains(todo)) {
                        deduplicatedTodoList.add(todo);
                    }
                });

        return "<b>${command.todo.foundlistcaption}:</b>\n"
                + todoListToString(deduplicatedTodoList);
    }

    private void addNewTodo(Chat chat, User user, List<String> tags, String text) {
        org.telegram.bot.domain.entities.Todo todo = new org.telegram.bot.domain.entities.Todo()
                .setChat(chat)
                .setUser(user)
                .setTodoText(text);
        List<TodoTag> todoTags = tags
                .stream().map(tag -> new TodoTag()
                        .setChat(chat)
                        .setUser(user)
                        .setTag(tag)
                        .setTodo(todo))
                .toList();
        todo.setTags(todoTags);

        todoService.save(todo);
    }

    private String todoListToString(List<org.telegram.bot.domain.entities.Todo> todoSet) {
        return todoSet
                .stream()
                .map(this::buildTodoString)
                .collect(Collectors.joining("\n"));
    }

    private String buildTodoString(org.telegram.bot.domain.entities.Todo todo) {
        return todo.getTodoText() + "\n" + tagListToString(todo.getTags()) + "/todo_del" + todo.getId() + "\n";
    }

    private String tagListToString(List<TodoTag> tags) {
        if (tags.isEmpty()) {
            return "";
        }

        StringBuilder buf = new StringBuilder();
        tags.forEach(tag -> buf.append(TAG_SYMBOL).append(tag.getTag()).append(" "));
        buf.append("\n");

        return buf.toString();
    }
}
