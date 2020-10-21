package org.telegram.bot;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.Token;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.AccessLevels;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.TokenService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Scanner;

@Component
@AllArgsConstructor
public class Bot extends TelegramLongPollingBot {

    private final Logger log = LoggerFactory.getLogger(Bot.class);

    private final ApplicationContext context;
    private final TokenService tokenService;
    private final CommandPropertiesService commandPropertiesService;
    private final UserService userService;
    private final ChatService chatService;

    @Override
    public void onUpdateReceived(Update update) {
        String textOfMessage = update.getMessage().getText();
        if (textOfMessage == null || textOfMessage.equals("")) {
            return;
        }

        User user = checkUserInfoUpdates(update.getMessage().getFrom());

        log.info("I have a message from " + user.getUsername() + " (" + user.getUserId() + "): " + textOfMessage);

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
        Token telegramBotApiToken = tokenService.get("telegramBotApiToken");
        if (telegramBotApiToken == null) {
            System.out.println("Enter telegram bot api token here: ");
            Scanner scanner = new Scanner(System.in);
            String newToken = scanner.nextLine();
            System.out.println("Enter the telegram id of the future owner of the bot: ");
            Long adminId = scanner.nextLong();

            telegramBotApiToken = new Token();
            telegramBotApiToken.setToken(newToken);
            telegramBotApiToken.setName("telegramBotApiToken");
            telegramBotApiToken.setDescription("Telegram bot api token");
            telegramBotApiToken = tokenService.save(telegramBotApiToken);

            User admin = new User();
            admin.setUserId(adminId);
            admin.setUsername("");
            admin.setAccessLevel(AccessLevels.ADMIN.getValue());
            userService.save(admin);
        }

        return telegramBotApiToken.getToken();
    }

    private User checkUserInfoUpdates(org.telegram.telegrambots.meta.api.objects.User userFrom) {
        String username = userFrom.getUserName();
        if (username == null) {
            username = userFrom.getFirstName();
        }

        Long userId = userFrom.getId().longValue();

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

    private AccessLevels getCurrentAccessLevel(Long userId, Long chatId) {
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
