package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.TodoService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@Slf4j
public class Todo implements Command<SendMessage> {

    private final Bot bot;
    private final TodoService todoService;
    private final UserService userService;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) throws BotException {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = cutCommandInText(message.getText());
        String responseText;
        if (textMessage == null) {
            responseText = getTodoList();
        } else {
            if (textMessage.startsWith("-")) {
                responseText = removeTodoElement(message, textMessage.substring(1));
            } else {
                try {
                    responseText = getTodoInfo(Long.parseLong(textMessage));
                } catch (NumberFormatException e) {
                    addNewTodo(textMessage, message.getFrom().getId());
                    responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
                }
            }
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.disableWebPagePreview();
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String getTodoList() {
        log.debug("Request to get all todo list");
        final StringBuilder buf = new StringBuilder();

        buf.append("${command.todo.caption}\n");
        todoService.getList().forEach(todo -> buf.append(buildTodoStringLine(todo)));

        return buf.toString();
    }

    private String removeTodoElement(Message message, String id) {
        long todoId;
        try {
            todoId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        log.debug("Request to delete Todo by id " + todoId);
        if (!userService.isUserHaveAccessForCommand(
                userService.get(message.getFrom().getId()).getAccessLevel(),
                AccessLevel.ADMIN.getValue())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_ACCESS));
        }

        if (todoService.remove(todoId)) {
            return "${command.todo.deleted}";
        } else {
            return speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        }
    }

    private String getTodoInfo(Long id) {
        log.debug("Request to get Todo by id {}", id);
        org.telegram.bot.domain.entities.Todo todo = todoService.get(id);
        if (todo == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return buildTodoStringLine(todo);
    }

    private void addNewTodo(String todoText, Long userId) {
        log.debug("Request to add new Todo");

        org.telegram.bot.domain.entities.Todo todo = new org.telegram.bot.domain.entities.Todo();
        todo.setUser(new User().setUserId(userId));
        todo.setTodoText(todoText);

        todoService.save(todo);
    }

    private String buildTodoStringLine(org.telegram.bot.domain.entities.Todo todo) {
        return todo.getId() + ") " + todo.getTodoText() + " (" + todo.getUser().getUsername() + ")\n";
    }
}
