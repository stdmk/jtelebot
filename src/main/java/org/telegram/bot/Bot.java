package org.telegram.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.bot.commands.Command;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.mapper.telegram.request.RequestMapper;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.TelegramUtils;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@RequiredArgsConstructor
@Component
@Slf4j
public class Bot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final ApplicationContext context;
    private final MessageAnalyzerExecutor messageAnalyzerExecutor;
    private final BotStats botStats;
    private final PropertiesConfig propertiesConfig;
    private final RequestMapper requestMapper;
    private final CommandPropertiesService commandPropertiesService;
    private final UserService userService;
    private final UserStatsService userStatsService;
    private final CommandWaitingService commandWaitingService;
    private final DisableCommandService disableCommandService;
    @Lazy
    private final LogService logService;
    private final Parser parser;
    private final TelegramClient telegramClient;

    @Override
    public void consume(Update update) {
        botStats.incrementReceivedMessages();

        BotRequest botRequest = requestMapper.toBotRequest(update);
        Message message = botRequest.getMessage();

        logService.log(message);

        if (TelegramUtils.isUnsupportedMessage(message)) {
            return;
        }

        userStatsService.updateEntitiesInfo(message);

        Long chatId = message.getChatId();
        Long userId = message.getUser().getUserId();
        Chat chatEntity = message.getChat();
        org.telegram.bot.domain.entities.User userEntity = message.getUser();

        AccessLevel userAccessLevel = userService.getCurrentAccessLevel(userId, chatId);
        if (userAccessLevel.equals(AccessLevel.BANNED)) {
            log.info("Banned user. Ignoring...");
            return;
        }

        processRequest(botRequest, chatEntity, userEntity, userAccessLevel, true);
    }

    public void processRequestWithoutAnalyze(BotRequest botRequest) {
        Message message = botRequest.getMessage();
        AccessLevel userAccessLevel = userService.getCurrentAccessLevel(message.getUser().getUserId(), message.getChatId());

        processRequest(botRequest, message.getChat(), message.getUser(), userAccessLevel, false);
    }

    public void processRequest(BotRequest botRequest) {
        Message message = botRequest.getMessage();
        AccessLevel userAccessLevel = userService.getCurrentAccessLevel(message.getUser().getUserId(), message.getChatId());

        processRequest(botRequest, message.getChat(), message.getUser(), userAccessLevel, true);
    }

    public void processRequest(BotRequest botRequest, Chat chat, org.telegram.bot.domain.entities.User user, AccessLevel userAccessLevel, boolean analyze) {
        if (analyze) {
            messageAnalyzerExecutor.analyzeMessageAsync(botRequest, userAccessLevel);
        }

        CommandProperties commandProperties = getCommandProperties(chat, user, botRequest.getMessage().getText());
        if (commandProperties == null || disableCommandService.get(chat, commandProperties) != null) {
            return;
        }

        Command command = getCommand(commandProperties);
        if (command == null) {
            return;
        }

        if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), commandProperties.getAccessLevel())) {
            userStatsService.incrementUserStatsCommands(chat, user, commandProperties);
            parser.parseAsync(botRequest, command);
        }
    }

    @Override
    public String getBotToken() {
        return propertiesConfig.getTelegramBotApiToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }


    private CommandProperties getCommandProperties(Chat chat, org.telegram.bot.domain.entities.User user, String textOfMessage) {
        CommandWaiting commandWaiting = commandWaitingService.get(chat, user);
        if (commandWaiting != null) {
            return commandPropertiesService.getCommand(commandWaiting.getCommandName());
        } else if (textOfMessage != null) {
            return commandPropertiesService.findCommandInText(textOfMessage, this.getBotUsername());
        } else {
            return null;
        }
    }

    private Command getCommand(CommandProperties commandProperties) {
        try {
            return (Command) context.getBean(commandProperties.getClassName());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        botStats.incrementErrors(commandProperties, "Missing Command implementation of " + commandProperties);

        return null;
    }

    public String getBotUsername() {
        String botUserName = propertiesConfig.getTelegramBotUsername();
        if (botUserName == null) {
            User botUser;
            try {
                botUser = telegramClient.execute(new GetMe());
                botUserName = botUser.getUserName();
                propertiesConfig.setTelegramBotUsername(botUserName);
            } catch (TelegramApiException e) {
                botUserName = "jtelebot";
            }
        }

        return botUserName;
    }

    public void sendMessage(TextResponse textResponse) {
        parser.executeAsync(textResponse);
    }

    public void sendDocument(FileResponse fileResponse) {
        parser.executeAsync(fileResponse);
    }

    public InputStream getInputStreamFromTelegramFile(String fileId) throws TelegramApiException, IOException {
        String filePath = telegramClient.execute(new GetFile(fileId)).getFilePath();
        String fileUrl = new File(null, null, null, filePath).getFileUrl(this.getBotToken());
        return new URL(fileUrl).openStream();
    }

    public void sendUploadPhoto(Long chatId) {
        sendAction(chatId, ActionType.UPLOAD_PHOTO);
    }

    public void sendUploadVideo(Long chatId) {
        sendAction(chatId, ActionType.UPLOAD_VIDEO);
    }

    public void sendUploadDocument(Long chatId) {
        sendAction(chatId, ActionType.UPLOAD_DOCUMENT);
    }

    public void sendTyping(Long chatId) {
        sendAction(chatId, ActionType.TYPING);
    }

    public void sendLocation(Long chatId) {
        sendAction(chatId, ActionType.FIND_LOCATION);
    }

    public void sendAction(Long chatId, ActionType action) {
        SendChatAction sendChatAction = new SendChatAction(chatId.toString(), action.toString());

        try {
            telegramClient.execute(sendChatAction);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(sendChatAction, e, "failed to send Action");
            log.error("Error: cannot send chat action: {}", e.getMessage());
        }
    }

}
