package org.telegram.bot;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.services.*;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
@AllArgsConstructor
public class Bot extends TelegramLongPollingBot {

    private final Logger log = LoggerFactory.getLogger(Bot.class);

    private final List<TextAnalyzer> textAnalyzerList;
    private final ApplicationContext context;
    private final BotStats botStats;

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
        Boolean spyMode = propertiesConfig.getSpyMode();

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

        botStats.incrementReceivedMessages();

        Long chatId = message.getChatId();
        Long userId = user.getId();
        log.info("From " + chatId + " (" + user.getUserName() + "-" + userId + "): " + textOfMessage);
        if (chatId > 0 && spyMode != null && spyMode) {
            reportToAdmin(message);
        }

        AccessLevel userAccessLevel = userService.getCurrentAccessLevel(userId, chatId);
        if (userAccessLevel.equals(AccessLevel.BANNED)) {
            log.info("Banned user. Ignoring...");
            return;
        }

        userStatsService.updateEntitiesInfo(message, editedMessage);

        if (textOfMessage != null) {
            textAnalyzerList.forEach(textAnalyzer -> textAnalyzer.analyze(this, (CommandParent<?>) textAnalyzer, update));
        }

        CommandProperties commandProperties;
        CommandWaiting commandWaiting = commandWaitingService.get(chatService.get(chatId), userService.get(userId));
        if (commandWaiting != null) {
            commandProperties = commandPropertiesService.getCommand(commandWaiting.getCommandName());
        } else if (textOfMessage != null) {
            commandProperties = commandPropertiesService.findCommandInText(textOfMessage, this.getBotUsername());
        } else {
            return;
        }

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
            userStatsService.incrementUserStatsCommands(chatService.get(chatId), userService.get(userId), commandProperties);
            Parser parser = new Parser(this, command, update, botStats);
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

    private void reportToAdmin(Message message) {
        Long adminId = propertiesConfig.getAdminId();
        if (adminId.equals(message.getFrom().getId()) || this.getBotUsername().equals(message.getFrom().getUserName())) {
            return;
        }

        SendMessage sendMessage = new SendMessage();

        sendMessage.setChatId(propertiesConfig.getAdminId().toString());
        sendMessage.setText("Received a message from (" + message.getFrom().getId() + ") @" + message.getFrom().getUserName() + ": " + message.getText());

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
