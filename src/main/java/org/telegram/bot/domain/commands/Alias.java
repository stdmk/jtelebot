package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.Parser;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
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
public class Alias implements CommandParent<SendMessage>, TextAnalyzer {

    private final ApplicationContext context;
    private final BotStats botStats;

    private final AliasService aliasService;
    private final UserService userService;
    private final UserStatsService userStatsService;
    private final CommandPropertiesService commandPropertiesService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        Chat chat = new Chat().setChatId(message.getChatId());
        User user = new User().setUserId(message.getFrom().getId());

        if (cutCommandInText(message.getText()) != null) {
            return null;
        }

        log.debug("Request to get list of user {} and chat {}", user, chat);
        StringBuilder buf = new StringBuilder("*Список твоих алиасов:*\n");

        aliasService.getByChatAndUser(chat, user)
                .forEach(alias -> buf
                        .append(alias.getId()).append(". ")
                        .append(alias.getName()).append(" - `")
                        .append(alias.getValue()).append("`\n"));

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setText(buf.toString());

        return sendMessage;
    }

    @Override
    public void analyze(Bot bot, CommandParent<?> command, Update update) {
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

                    Parser parser = new Parser(bot, (CommandParent<?>) context.getBean(commandProperties.getClassName()), newUpdate, botStats);
                    parser.start();
                }
            }
            log.debug("The alias found is not a command");
        }
        log.debug("No aliases found");
    }
}
