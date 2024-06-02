package org.telegram.bot;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.commands.Command;
import org.telegram.bot.commands.MessageAnalyzer;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.mapper.TelegramObjectMapper;
import org.telegram.bot.services.*;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@Slf4j
public class Bot extends TelegramLongPollingBot {

    private final List<MessageAnalyzer> messageAnalyzerList;
    private final ApplicationContext context;
    private final BotStats botStats;
    private final PropertiesConfig propertiesConfig;
    private final TelegramObjectMapper telegramObjectMapper;
    private final CommandPropertiesService commandPropertiesService;
    private final UserService userService;
    private final UserStatsService userStatsService;
    private final CommandWaitingService commandWaitingService;
    private final DisableCommandService disableCommandService;
    private final SpeechService speechService;
    private final SpyModeService spyModeService;
    private final Parser parser;

    public Bot(@Lazy List<MessageAnalyzer> messageAnalyzerList,
               ApplicationContext context,
               BotStats botStats,
               PropertiesConfig propertiesConfig,
               TelegramObjectMapper telegramObjectMapper,
               CommandPropertiesService commandPropertiesService,
               UserService userService, UserStatsService userStatsService,
               CommandWaitingService commandWaitingService,
               DisableCommandService disableCommandService,
               SpeechService speechService,
               SpyModeService spyModeService,
               @Value("${telegramBotApiToken}") String botToken, Parser parser) {
        super(botToken);
        this.messageAnalyzerList = messageAnalyzerList;
        this.context = context;
        this.botStats = botStats;
        this.propertiesConfig = propertiesConfig;
        this.telegramObjectMapper = telegramObjectMapper;
        this.commandPropertiesService = commandPropertiesService;
        this.userService = userService;
        this.userStatsService = userStatsService;
        this.commandWaitingService = commandWaitingService;
        this.disableCommandService = disableCommandService;
        this.speechService = speechService;
        this.spyModeService = spyModeService;
        this.parser = parser;
    }

    @Override
    public void onUpdateReceived(Update update) {
        botStats.incrementReceivedMessages();

        BotRequest botRequest = telegramObjectMapper.toBotRequest(update);

        logReceivedMessage(botRequest);

        Message message = botRequest.getMessage();
        Long chatId = message.getChatId();
        Long userId = message.getUser().getUserId();
        Chat chatEntity = message.getChat();
        org.telegram.bot.domain.entities.User userEntity = new org.telegram.bot.domain.entities.User().setUserId(userId);

        AccessLevel userAccessLevel = userService.getCurrentAccessLevel(userId, chatId);
        if (userAccessLevel.equals(AccessLevel.BANNED)) {
            log.info("Banned user. Ignoring...");
            return;
        }

        userStatsService.updateEntitiesInfo(message);

        analyzeMessage(botRequest, userAccessLevel);

        CommandProperties commandProperties = getCommandProperties(chatEntity, userEntity, message.getText());
        if (commandProperties == null || disableCommandService.get(chatEntity, commandProperties) != null) {
            return;
        }

        Command command = getCommand(commandProperties);
        if (command == null) {
            return;
        }

        if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), commandProperties.getAccessLevel())) {
            userStatsService.incrementUserStatsCommands(chatEntity, userEntity, commandProperties);
            parser.parseAsync(botRequest, command);
        }
    }

    private void logReceivedMessage(BotRequest botRequest) {
        Message message = botRequest.getMessage();
        org.telegram.bot.domain.entities.User user = message.getUser();

        String textOfMessage = message.getText();
        Boolean spyMode = propertiesConfig.getSpyMode();
        Long chatId = message.getChatId();
        Long userId = user.getUserId();
        log.info("From " + chatId + " (" + user.getUsername() + "-" + userId + "): " + textOfMessage);
        if (chatId > 0 && spyMode != null && spyMode) {
            reportToAdmin(user, textOfMessage);
        }
    }

    private void analyzeMessage(BotRequest botRequest, AccessLevel userAccessLevel) {
        messageAnalyzerList.forEach(messageAnalyzer -> {
            CommandProperties analyzerCommandProperties = commandPropertiesService.getCommand(messageAnalyzer.getClass());
            if (analyzerCommandProperties == null
                    || userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), analyzerCommandProperties.getAccessLevel())) {
                parser.executeMethod(botRequest, messageAnalyzer.analyze(botRequest));
            }
        });
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

        return null;
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

    private void reportToAdmin(org.telegram.bot.domain.entities.User user, String textMessage) {
        Long adminId = propertiesConfig.getAdminId();
        if (adminId.equals(user.getUserId()) || this.getBotUsername().equals(user.getUsername())) {
            return;
        }

        TextResponse textResponse = spyModeService.generateResponse(user, textMessage);

        this.sendMessage(textResponse);
    }

    public void sendMessage(TextResponse textResponse) {
        parser.executeMethod(textResponse);
    }

    public void sendDocument(FileResponse fileResponse) {
        parser.executeMethod(fileResponse);
    }

    public byte[] getFileFromTelegram(String fileId) {
        try {
            return IOUtils.toByteArray(getInputStreamFromTelegramFile(fileId));
        } catch (IOException e) {
            log.error("Failed to get file from telegram", e);
            botStats.incrementErrors(fileId, e, "Failed to get file from telegram");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

    public InputStream getInputStreamFromTelegramFile(String fileId) {
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);

        try {
            return this.downloadFileAsStream(this.execute(getFile).getFilePath());
        } catch (TelegramApiException e) {
            log.error("Failed to get file from telegram", e);
            botStats.incrementErrors(fileId, e, "Failed to get file from telegram");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

    public void sendUploadPhoto(Long chatId) {
        sendAction(chatId, ActionType.UPLOADPHOTO);
    }

    public void sendUploadVideo(Long chatId) {
        sendAction(chatId, ActionType.UPLOADVIDEO);
    }

    public void sendUploadDocument(BotRequest request) {
        sendAction(request.getMessage().getChatId(), ActionType.UPLOADDOCUMENT);
    }

    public void sendUploadDocument(Long chatId) {
        sendAction(chatId, ActionType.UPLOADDOCUMENT);
    }

    public void sendTyping(Long chatId) {
        sendAction(chatId, ActionType.TYPING);
    }

    public void sendLocation(Long chatId) {
        sendAction(chatId, ActionType.FINDLOCATION);
    }

    public void sendAction(Long chatId, ActionType action) {
        SendChatAction sendChatAction = new SendChatAction();
        sendChatAction.setChatId(chatId);
        sendChatAction.setAction(action);

        try {
            execute(sendChatAction);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(sendChatAction, e, "ошибка при отправке Action");
            log.error("Error: cannot send chat action: {}", e.getMessage());
        }
    }

}
