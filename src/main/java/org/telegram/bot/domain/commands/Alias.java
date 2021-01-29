package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.Parser;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.telegram.bot.utils.TextUtils.getPotentialCommandInText;

@Component
@AllArgsConstructor
public class Alias implements CommandParent<SendMessage>, TextAnalyzer {

    private final ApplicationContext context;

    private final AliasService aliasService;
    private final ChatService chatService;
    private final UserService userService;
    private final UserStatsService userStatsService;
    private final CommandPropertiesService commandPropertiesService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        StringBuilder buf = new StringBuilder("*Список твоих алиасов:*\n");

        aliasService.get(chatService.get(message.getChatId()), userService.get(message.getFrom().getId()))
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
        String potentialCommand = getPotentialCommandInText(message.getText());
        if (potentialCommand != null) {
            Chat chat = chatService.get(message.getChatId());
            User user = userService.get(message.getFrom().getId());

            org.telegram.bot.domain.entities.Alias alias = aliasService.get(chat, user, potentialCommand);

            if (alias != null) {
                String textMessage = alias.getValue();
                update.getMessage().setText(textMessage);
                CommandProperties commandProperties = commandPropertiesService.findCommandInText(textMessage, bot.getBotUsername());
                if (commandProperties != null) {
                    if (userService.isUserHaveAccessForCommand(userService.getCurrentAccessLevel(user.getUserId(), chat.getChatId()).getValue(), commandProperties.getAccessLevel())) {
                        userStatsService.incrementUserStatsCommands(chatService.get(chat.getChatId()), userService.get(user.getUserId()));

                        Parser parser = new Parser(bot, (CommandParent<?>) context.getBean(commandProperties.getClassName()), update);
                        parser.start();
                    }
                }
            }
        }
    }
}
