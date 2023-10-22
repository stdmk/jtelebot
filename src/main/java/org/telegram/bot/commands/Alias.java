package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.services.AliasService;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@Slf4j
public class Alias implements Command<SendMessage>, TextAnalyzer {

    private final MessageSource messageSource;
    private final ApplicationContext context;
    private final Bot bot;

    private final AliasService aliasService;
    private final UserService userService;
    private final UserStatsService userStatsService;
    private final CommandPropertiesService commandPropertiesService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        Long chatId = message.getChatId();

        bot.sendTyping(chatId);

        Chat chat = new Chat().setChatId(chatId);
        User user = new User().setUserId(message.getFrom().getId());

        if (cutCommandInText(message.getText()) != null) {
            return null;
        }

        log.debug("Request to get list of user {} and chat {}", user, chat);
        StringBuilder buf = new StringBuilder("*${command.alias.aliaslist}:*\n");

        aliasService.getByChatAndUser(chat, user)
                .forEach(alias -> buf
                        .append(alias.getId()).append(". ")
                        .append(alias.getName()).append(" - `")
                        .append(alias.getValue()).append("`\n"));

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setText(buf.toString());

        return sendMessage;
    }

    @Override
    public void analyze(Command<?> command, Update update) {
        Message message = getMessageFromUpdate(update);
        Chat chat = new Chat().setChatId(message.getChatId());
        User user = new User().setUserId(message.getFrom().getId());
        log.debug("Initialization of alias search for user {} and chat {}", user, chat);

        org.telegram.bot.domain.entities.Alias alias = aliasService.get(chat, user, message.getText());

        if (alias != null) {
            Update newUpdate = copyUpdate(update);
            if (newUpdate == null) {
                log.error("Failed to get a copy of update");
                return;
            }

            String aliasValue = alias.getValue();
            Message newMessage = getMessageFromUpdate(newUpdate);
            newMessage.setText(aliasValue);
            CommandProperties commandProperties = commandPropertiesService.findCommandInText(aliasValue, bot.getBotUsername());

            if (commandProperties != null) {
                if (userService.isUserHaveAccessForCommand(userService.getCurrentAccessLevel(user.getUserId(), chat.getChatId()).getValue(), commandProperties.getAccessLevel())) {
                    userStatsService.incrementUserStatsCommands(chat, user);
                    bot.parseAsync(newUpdate, (Command<?>) context.getBean(commandProperties.getClassName()));
                }
            }
            log.debug("The alias found is not a command");
        }
        log.debug("No aliases found");
    }
}
