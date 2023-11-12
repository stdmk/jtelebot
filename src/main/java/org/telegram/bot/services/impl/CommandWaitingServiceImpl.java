package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.CommandWaitingRepository;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.telegrambots.meta.api.objects.Message;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandWaitingServiceImpl implements CommandWaitingService {

    private final CommandWaitingRepository commandWaitingRepository;
    private final CommandPropertiesService commandPropertiesService;
    private final InternationalizationService internationalizationService;

    @Override
    public CommandWaiting get(Chat chat, User user) {
        log.debug("Request to get CommandWaiting by chat: {} and user: {}", chat, user);
        return commandWaitingRepository.findByChatAndUser(chat, user);
    }

    @Override
    public String getText(Message message) {
        CommandWaiting commandWaiting = get(new Chat().setChatId(message.getChatId()), new User().setUserId(message.getFrom().getId()));

        if (commandWaiting == null) {
            return null;
        }

        remove(commandWaiting);

        return message.getText();
    }

    @Override
    public void add(Message message, Class<?> commandClass) {
        Chat chat = new Chat().setChatId(message.getChatId());
        User user = new User().setUserId(message.getFrom().getId());

        add(chat, user, commandClass, commandPropertiesService.getCommand(commandClass).getCommandName());
    }

    @Override
    public void add(Chat chat, User user, Class<?> commandClass, String commandText) {
        String commandName = commandPropertiesService.getCommand(commandClass).getCommandName();

        CommandWaiting commandWaiting = get(chat, user);
        if (commandWaiting == null) {
            commandWaiting = new CommandWaiting()
                    .setChat(chat)
                    .setUser(user);
        }

        commandWaiting
                .setCommandName(commandName)
                .setIsFinished(false)
                .setTextMessage("/" + internationalizationService.internationalize(commandText, null) + " ");

        save(commandWaiting);
    }

    @Override
    public CommandWaiting save(CommandWaiting commandWaiting) {
        log.debug("Request to save CommandWaitingId {} ", commandWaiting);
        return commandWaitingRepository.save(commandWaiting);
    }

    @Override
    public void remove(CommandWaiting commandWaiting) {
        if (commandWaiting == null) {
            return;
        }
        log.debug("Request to remove CommandWaiting {} ", commandWaiting);
        commandWaitingRepository.delete(commandWaiting);
    }

    @Override
    @Transactional
    public void remove(Chat chat, User user) {
        log.debug("Request to remove CommandWaiting for Chat {} and User {}", chat, user);
        commandWaitingRepository.deleteByChatAndUser(chat, user);
    }
}
