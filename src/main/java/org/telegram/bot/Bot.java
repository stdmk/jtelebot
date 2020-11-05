package org.telegram.bot;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.enums.AccessLevels;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@AllArgsConstructor
public class Bot extends TelegramLongPollingBot {

    private final Logger log = LoggerFactory.getLogger(Bot.class);

    private final ApplicationContext context;
    private final PropertiesConfig propertiesConfig;
    private final CommandPropertiesService commandPropertiesService;
    private final UserService userService;
    private final UserStatsService userStatsService;

    @Override
    public void onUpdateReceived(Update update) {
        String textOfMessage = update.getMessage().getText();

        User user = update.getMessage().getFrom();
        log.info("From " + update.getMessage().getChatId() + " (" + user.getUserName() + "-" + user.getId() + "): " + textOfMessage);

        AccessLevels userAccessLevel = userService.getCurrentAccessLevel(user.getId(), update.getMessage().getChatId());
        if (userAccessLevel.equals(AccessLevels.BANNED)) {
            log.info("Banned user. Ignoring...");
            return;
        }

        userStatsService.updateEntitiesInfo(update);

        if (textOfMessage == null || textOfMessage.equals("")) {
            return;
        }

        CommandProperties commandProperties = commandPropertiesService.findCommandInText(textOfMessage, this.getBotUsername());
        if (commandProperties == null) {
            return;
        }

        CommandParent<?> command = null;
        try {
            command = (CommandParent<?>) context.getBean(commandProperties.getClassName());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), commandProperties.getAccessLevel())) {
            userStatsService.incrementUserStatsCommands(update.getMessage().getChatId(), user.getId());
            Parser parser = new Parser(this, command, update);
            parser.start();
        }

    }

    @Override
    public String getBotUsername() {
        User botUser;
        try {
            botUser = this.execute(new GetMe());
        } catch (TelegramApiException e) {
            return "jtelebot";
        }

        return botUser.getUserName();
    }

    @Override
    public String getBotToken() {
        String telegramBotApiToken = propertiesConfig.getTelegramBotApiToken();
        if (telegramBotApiToken.equals("")) {
            log.info("Can't find telegram bot api token. See the properties.properties file");
        }

        return telegramBotApiToken;
    }
}
