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
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@AllArgsConstructor
public class Bot extends TelegramLongPollingBot {

    private final Logger log = LoggerFactory.getLogger(Bot.class);

    private final ApplicationContext context;
    private final PropertiesService propertiesService;
    private final CommandPropertiesService commandPropertiesService;
    private final UserService userService;
    private final ChatService chatService;
    private final UserStatsService userStatsService;

    @Override
    public void onUpdateReceived(Update update) {
        String textOfMessage = update.getMessage().getText();
        if (textOfMessage == null || textOfMessage.equals("")) {
            return;
        }

        userStatsService.updateEntitiesInfo(update);

        User user = update.getMessage().getFrom();
        log.info("From " + update.getMessage().getChatId() + " (" + user.getUserName() + "-" + user.getId() + "): " + textOfMessage);

        AccessLevels userAccessLevel = getCurrentAccessLevel(user.getId(), update.getMessage().getChatId());
        if (userAccessLevel.equals(AccessLevels.BANNED)) {
            log.info("Banned user. Ignoring...");
            return;
        }

        CommandProperties commandProperties = commandPropertiesService.findCommandInText(textOfMessage);
        if (commandProperties == null) {
            return;
        }

        CommandParent command = null;
        try {
            command = (CommandParent) context.getBean(commandProperties.getClassName());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        if (isUserHaveAccessForCommand(userAccessLevel.getValue(), commandProperties.getAccessLevel())) {
            Parser parser = new Parser(this, command, update);
            parser.start();
        }

    }

    @Override
    public String getBotUsername() {
        return "jtelebot";
    }

    @Override
    public String getBotToken() {
        String telegramBotApiToken = propertiesService.get("telegramBotApiToken");
        if (telegramBotApiToken.equals("null")) {
            log.info("Can't find telegram bot api token. See the properties.properties file");
        }

        return telegramBotApiToken;
    }

    private AccessLevels getCurrentAccessLevel(Integer userId, Long chatId) {
        AccessLevels level = AccessLevels.BANNED;

        Integer userLevel = userService.getUserAccessLevel(userId);
        if (userLevel < 0) {
            return level;
        }
        Integer chatLevel = chatService.getChatAccessLevel(chatId);

        if (userLevel > chatLevel) {
            level = AccessLevels.getUserLevelByValue(userLevel);
        } else {
            level = AccessLevels.getUserLevelByValue(chatLevel);
        }

        return level;
    }

    private Boolean isUserHaveAccessForCommand(Integer userAccessLevel, Integer commandAccessLevel) {
        return userAccessLevel >= commandAccessLevel;
    }
}
