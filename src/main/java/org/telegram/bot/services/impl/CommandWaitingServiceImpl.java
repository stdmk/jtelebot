package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.CommandWaitingRepository;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.objects.Message;

@Service
@AllArgsConstructor
public class CommandWaitingServiceImpl implements CommandWaitingService {

    private final Logger log = LoggerFactory.getLogger(CommandWaitingServiceImpl.class);

    private final CommandWaitingRepository commandWaitingRepository;

    private final ChatService chatService;
    private final UserService userService;
    private final CommandPropertiesService commandPropertiesService;

    @Override
    public CommandWaiting get(Chat chat, User user) {
        log.debug("Request to get CommandWaiting by chat: {} and user: {}", chat, user);
        return commandWaitingRepository.findByChatAndUser(chat, user);
    }

    @Override
    public CommandWaiting get(Long chatId, Integer userId) {
        return get(chatService.get(chatId), userService.get(userId));
    }

    @Override
    public String getText(Message message) {
        CommandWaiting commandWaiting = get(message.getChatId(), message.getFrom().getId());

        if (commandWaiting == null) {
            return null;
        }

        remove(commandWaiting);

        return message.getText();

    }

    @Override
    public void add(Message message, Class<?> commandClass) {
        add(message, commandClass, commandPropertiesService.getCommand(commandClass).getCommandName());
    }

    @Override
    public void add(Message message, Class<?> commandClass, String commandText) {
        add(chatService.get(message.getChatId()), userService.get(message.getFrom().getId()), commandClass, commandText);
    }

    @Override
    public void add(Chat chat, User user, Class<?> commandClass, String commandText) {
        CommandWaiting commandWaiting = get(chat, user);
        if (commandWaiting == null) {
            commandWaiting = new CommandWaiting();
            commandWaiting.setChat(chat);
            commandWaiting.setUser(user);
        }

        commandWaiting.setCommandName(commandClass.getSimpleName());
        commandWaiting.setIsFinished(false);
        commandWaiting.setTextMessage("/" + commandText + " ");
        save(commandWaiting);
    }

    @Override
    public CommandWaiting save(CommandWaiting commandWaiting) {
        log.debug("Request to save CommandWaitingId {} ", commandWaiting);
        return commandWaitingRepository.save(commandWaiting);
    }

    @Override
    public void remove(CommandWaiting commandWaiting) {
        log.debug("Request to remove CommandWaiting {} ", commandWaiting);
        commandWaitingRepository.delete(commandWaiting);
    }
}
