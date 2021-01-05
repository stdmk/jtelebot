package org.telegram.bot;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
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
    private final ChatService chatService;
    private final UserStatsService userStatsService;
    private final CommandWaitingService commandWaitingService;

    @Override
    public void onUpdateReceived(Update update) {
        Message message;
        User user;
        String textOfMessage;
        boolean editedMessage = false;

        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            message = callbackQuery.getMessage();
            textOfMessage = callbackQuery.getData();
            user = callbackQuery.getFrom();
        } else {
            if (update.hasMessage()) {
                message = update.getMessage();
            } else if (update.hasEditedMessage()) {
                message = update.getEditedMessage();
                editedMessage = true;
            } else {
                return;
            }
            textOfMessage = message.getText();
            user = message.getFrom();
        }

        Long chatId = message.getChatId();
        Integer userId = user.getId();
        log.info("From " + chatId + " (" + user.getUserName() + "-" + userId + "): " + textOfMessage);

        AccessLevel userAccessLevel = userService.getCurrentAccessLevel(userId, chatId);
        if (userAccessLevel.equals(AccessLevel.BANNED)) {
            log.info("Banned user. Ignoring...");
            return;
        }

        userStatsService.updateEntitiesInfo(message, editedMessage);

        if (textOfMessage == null || textOfMessage.equals("")) {
            return;
        }

        CommandProperties commandProperties = commandPropertiesService.findCommandInText(textOfMessage, this.getBotUsername());
        if (commandProperties == null) {
            CommandWaiting commandWaiting = commandWaitingService.get(chatService.get(chatId), userService.get(userId));
            if (commandWaiting == null) {
                return;
            }
            commandProperties = commandPropertiesService.getCommand(commandWaiting.getCommandName());
            if (commandProperties == null) {
                return;
            }
        }

        CommandParent<?> command = null;
        try {
            command = (CommandParent<?>) context.getBean(commandProperties.getClassName());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), commandProperties.getAccessLevel())) {
            userStatsService.incrementUserStatsCommands(chatService.get(chatId), userService.get(userId));
            Parser parser = new Parser(this, command, update);
            parser.start();
        }

    }

    @Override
    public String getBotUsername() {
        String botUserName = propertiesConfig.getTelegramBotUsername();
        if (botUserName == null) {
            User botUser;
            try {
                botUser = this.execute(new GetMe());
                botUserName = botUser.getUserName();
                propertiesConfig.setTelegramBotUsername(botUserName);
            } catch (TelegramApiException e) {
                botUserName = "jtelebot";
            }
        }

        return botUserName;
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
