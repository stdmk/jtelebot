package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.LastCommand;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.services.LastCommandService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.bot.utils.ObjectCopier;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Repeat implements Command, MessageAnalyzer {

    private final ApplicationContext context;
    private final ObjectCopier objectCopier;

    private final UserService userService;
    private final UserStatsService userStatsService;
    private final LastCommandService lastCommandService;

    @Override
    public List<BotResponse> analyze(BotRequest request) {
        Message message = request.getMessage();

        if (message.hasText() && message.getText().equals(".")) {
            Chat chat = message.getChat();
            User user = message.getUser();
            LastCommand lastCommand = lastCommandService.get(chat);

            if (lastCommand != null) {
                CommandProperties commandProperties = lastCommand.getCommandProperties();
                log.debug("Request to repeat Command {}", commandProperties);

                if (userService.isUserHaveAccessForCommand(userService.getCurrentAccessLevel(user.getUserId(), chat.getChatId()).getValue(), commandProperties.getAccessLevel())) {
                    BotRequest newRequest = objectCopier.copyObject(request, BotRequest.class);
                    if (newRequest == null) {
                        return returnResponse();
                    }

                    newRequest.getMessage().setText(commandProperties.getCommandName());
                    userStatsService.incrementUserStatsCommands(chat, user);
                    return ((Command) context.getBean(commandProperties.getClassName())).parse(newRequest);
                }

                log.debug("User does not have access to with command");
            }
        }

        return returnResponse();
    }

    @Override
    public List<BotResponse> parse(BotRequest request) {
        return returnResponse();
    }
}
