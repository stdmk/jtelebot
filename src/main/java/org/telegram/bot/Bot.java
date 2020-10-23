package org.telegram.bot;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.AccessLevels;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

@Component
@AllArgsConstructor
public class Bot extends TelegramLongPollingBot {

    private final Logger log = LoggerFactory.getLogger(Bot.class);

    private final ApplicationContext context;
    private final PropertiesConfig propertiesConfig;
    private final CommandPropertiesService commandPropertiesService;
    private final UserService userService;
    private final ChatService chatService;

    @Override
    public void onUpdateReceived(Update update) {
        String textOfMessage = update.getMessage().getText();
        if (textOfMessage == null || textOfMessage.equals("")) {
            return;
        }

        Long chatId = update.getMessage().getChatId();
        User user = checkUserInfoUpdates(update.getMessage().getFrom());

        log.info("From " + chatId + " (" + user.getUsername() + "-" + user.getUserId() + "): " + textOfMessage);

        AccessLevels userAccessLevel = getCurrentAccessLevel(user.getUserId(), update.getMessage().getChatId());
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
        String telegramBotApiToken = propertiesConfig.getTelegramBotApiToken();
        if (telegramBotApiToken.equals("")) {
            log.info("Can't find telegram bot api token. See the properties.properties file");
            createDefaulPropertiesFile();
        }

        return telegramBotApiToken;
    }

    private User checkUserInfoUpdates(org.telegram.telegrambots.meta.api.objects.User userFrom) {
        String username = userFrom.getUserName();
        if (username == null) {
            username = userFrom.getFirstName();
        }

        Integer userId = userFrom.getId();

        User user = userService.get(userId);

        if (user == null) {
            user = new User();
            user.setUserId(userId);
            user.setUsername(username);
            user.setAccessLevel(AccessLevels.NEWCOMER.getValue());
            user = userService.save(user);
        }

        else if (!user.getUsername().equals(username)) {
            user.setUsername(username);
            user = userService.save(user);
        }

        return user;
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

    private void createDefaulPropertiesFile() {
        Properties properties = new Properties();
        properties.setProperty("telegramBotApiToken", "");
        properties.setProperty("adminId", "0");
        try {
            properties.store(new FileOutputStream("properties.properties"), null);
        } catch (IOException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        }
    }
}
