package org.telegram.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.domain.entities.Chat;
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
@Slf4j
public class Bot extends TelegramLongPollingBot {

    private static final Integer MESSAGE_EXPIRATION_TIME_SECONDS = 15;

    private final List<TextAnalyzer> textAnalyzerList;
    private final ApplicationContext context;
    private final BotStats botStats;

    private final PropertiesConfig propertiesConfig;
    private final CommandPropertiesService commandPropertiesService;
    private final UserService userService;
    private final UserStatsService userStatsService;
    private final CommandWaitingService commandWaitingService;
    private final DisableCommandService disableCommandService;
    private final SpyModeService spyModeService;

    public Bot(List<TextAnalyzer> textAnalyzerList,
               ApplicationContext context,
               BotStats botStats,
               PropertiesConfig propertiesConfig,
               CommandPropertiesService commandPropertiesService,
               UserService userService, UserStatsService userStatsService,
               CommandWaitingService commandWaitingService,
               DisableCommandService disableCommandService,
               SpyModeService spyModeService,
               @Value("${telegramBotApiToken}") String botToken) {
        super(botToken);
        this.textAnalyzerList = textAnalyzerList;
        this.context = context;
        this.botStats = botStats;
        this.propertiesConfig = propertiesConfig;
        this.commandPropertiesService = commandPropertiesService;
        this.userService = userService;
        this.userStatsService = userStatsService;
        this.commandWaitingService = commandWaitingService;
        this.disableCommandService = disableCommandService;
        this.spyModeService = spyModeService;
    }

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
                if (isThatAnOldMessage(message)) {
                    return;
                }
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
            reportToAdmin(user, textOfMessage);
        }

        AccessLevel userAccessLevel = userService.getCurrentAccessLevel(userId, chatId);
        if (userAccessLevel.equals(AccessLevel.BANNED)) {
            log.info("Banned user. Ignoring...");
            return;
        }

        userStatsService.updateEntitiesInfo(message, editedMessage);

        textAnalyzerList.forEach(textAnalyzer -> textAnalyzer.analyze(this, (CommandParent<?>) textAnalyzer, update));

        Chat chatEntity = new Chat().setChatId(chatId);
        org.telegram.bot.domain.entities.User userEntity = new org.telegram.bot.domain.entities.User().setUserId(userId);

        CommandProperties commandProperties;
        CommandWaiting commandWaiting = commandWaitingService.get(chatEntity, userEntity);
        if (commandWaiting != null) {
            commandProperties = commandPropertiesService.getCommand(commandWaiting.getCommandName());
        } else if (textOfMessage != null) {
            commandProperties = commandPropertiesService.findCommandInText(textOfMessage, this.getBotUsername());
        } else {
            return;
        }

        if (commandProperties == null || disableCommandService.get(chatEntity, commandProperties) != null) {
            return;
        }

        CommandParent<?> command = null;
        try {
            command = (CommandParent<?>) context.getBean(commandProperties.getClassName());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), commandProperties.getAccessLevel())) {
            userStatsService.incrementUserStatsCommands(chatEntity, userEntity, commandProperties);
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

    private void reportToAdmin(User user, String textMessage) {
        Long adminId = propertiesConfig.getAdminId();
        if (adminId.equals(user.getId()) || this.getBotUsername().equals(user.getUserName())) {
            return;
        }

        SendMessage sendMessage = spyModeService.generateMessage(user, textMessage);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private boolean isThatAnOldMessage(Message message) {
        Integer editDate = message.getEditDate();
        if (editDate != null) {
            return editDate - message.getDate() > MESSAGE_EXPIRATION_TIME_SECONDS;
        }

        return false;
    }
}
