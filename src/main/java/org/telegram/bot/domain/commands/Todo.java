package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.User;
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
@RequiredArgsConstructor
@Slf4j
public class Todo implements CommandParent<SendMessage> {

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
            log.debug("Request to get all todo list");
            final StringBuilder buf = new StringBuilder();

            buf.append("Туду лист\n");
            todoService.getList().forEach(todo -> buf.append(buildTodoStringLine(todo)));

            responseText = buf.toString();
        } else {
            if (textMessage.startsWith("-")) {
                long todoId;
                try {
                    todoId = Long.parseLong(textMessage.substring(1));
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
                    responseText = "Задача успешно удалена";
                } else {
                    responseText = speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
                }
            } else {
                try {
                    Long todoId = Long.parseLong(textMessage);

                    log.debug("Request to get Todo by id {}", todoId);
                    org.telegram.bot.domain.entities.Todo todo = todoService.get(todoId);
                    if (todo == null) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }

                    responseText = buildTodoStringLine(todo);
                } catch (NumberFormatException e) {
                    log.debug("Request to add new Todo");

                    org.telegram.bot.domain.entities.Todo todo = new org.telegram.bot.domain.entities.Todo();
                    todo.setUser(new User().setUserId(message.getFrom().getId()));
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
