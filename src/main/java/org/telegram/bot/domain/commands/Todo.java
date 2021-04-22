package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.TodoService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@AllArgsConstructor
public class Todo implements CommandParent<SendMessage> {

    private final Logger log = LoggerFactory.getLogger(Todo.class);

    private final TodoService todoService;
    private final UserService userService;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) throws BotException {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        String responseText;
        if (textMessage == null) {
            log.debug("Request to get all todo list");
            final StringBuilder buf = new StringBuilder();
            buf.append("Туду лист\n");
            todoService.getList().forEach(todo -> buf.append(buildTodoStringLine(todo)));
            responseText = buf.toString();
        } else {
            if (textMessage.charAt(0) == '-') {
                long todoId;
                try {
                    todoId = Long.parseLong(textMessage.substring(1));
                } catch (NumberFormatException e) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }
                log.debug("Request to delete todo by id " + todoId);
                if (!userService.isUserHaveAccessForCommand(
                        userService.get(message.getFrom().getId()).getAccessLevel(),
                        AccessLevel.ADMIN.getValue())) {
                    throw new BotException("Удалять может только админ");
                }
                if (todoService.remove(todoId)) {
                    responseText = "Задача успешно удалена";
                } else {
                    responseText = speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
                }
            } else {
                try {
                    Long todoId = Long.parseLong(textMessage);
                    org.telegram.bot.domain.entities.Todo todo = todoService.get(todoId);
                    if (todo == null) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                    responseText = buildTodoStringLine(todo);
                } catch (NumberFormatException e) {
                    log.debug("Request to add new todo");
                    org.telegram.bot.domain.entities.Todo todo = new org.telegram.bot.domain.entities.Todo();
                    todo.setUser(userService.get(message.getFrom().getId()));
                    todo.setTodoText(textMessage);
                    todoService.save(todo);
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

    private String buildTodoStringLine(org.telegram.bot.domain.entities.Todo todo) {
        return todo.getId() + ") " + todo.getTodoText() + " (" + todo.getUser().getUsername() + ")\n";
    }
}
