package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.LastCommand;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.services.LastCommandService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.bot.utils.ObjectCopier;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@Slf4j
public class Repeat implements TextAnalyzer, Command<PartialBotApiMethod<?>> {

    private final Bot bot;
    private final ApplicationContext context;
    private final ObjectCopier objectCopier;

    private final UserService userService;
    private final UserStatsService userStatsService;
    private final LastCommandService lastCommandService;

    @Override
    public void analyze(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = message.getText();

        if (textMessage != null && textMessage.equals(".")) {
            Chat chat = new Chat().setChatId(message.getChatId());
            User user = new User().setUserId(message.getFrom().getId());
            LastCommand lastCommand = lastCommandService.get(chat);

            if (lastCommand != null) {
                CommandProperties commandProperties = lastCommand.getCommandProperties();
                log.debug("Request to repeat Command {}", commandProperties);

                if (userService.isUserHaveAccessForCommand(userService.getCurrentAccessLevel(user.getUserId(), chat.getChatId()).getValue(), commandProperties.getAccessLevel())) {
                    Update newUpdate = objectCopier.copyObject(update, Update.class);
                    if (newUpdate == null) {
                        return;
                    }

                    newUpdate.getMessage().setText(lastCommand.getCommandProperties().getCommandName());
                    userStatsService.incrementUserStatsCommands(chat, user);
                    bot.parseAsync(newUpdate, (Command<?>) context.getBean(commandProperties.getClassName()));
                }

                log.debug("User does not have access to with command");
            }
        }
    }

    @Override
    public PartialBotApiMethod<?> parse(Update update) {
        return null;
    }
}
