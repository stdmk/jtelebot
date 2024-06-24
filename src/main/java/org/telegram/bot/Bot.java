package org.telegram.bot;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.bot.commands.Command;
import org.telegram.bot.commands.MessageAnalyzer;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.BotStats;
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
import org.telegram.bot.utils.TelegramUtils;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@Slf4j
public class Bot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

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
    private final TelegramClient telegramClient;

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
               Parser parser,
               TelegramClient telegramClient) {
        super();
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
        this.telegramClient = telegramClient;
    }

    @Override
    public void consume(Update update) {
        botStats.incrementReceivedMessages();

        BotRequest botRequest = telegramObjectMapper.toBotRequest(update);
        Message message = botRequest.getMessage();
        if (TelegramUtils.isUnsupportedMessage(message)) {
            return;
        }
        logReceivedMessage(botRequest);

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

    @Override
    public String getBotToken() {
        return propertiesConfig.getTelegramBotApiToken();
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

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    private void analyzeMessage(BotRequest botRequest, AccessLevel userAccessLevel) {
        messageAnalyzerList.forEach(messageAnalyzer -> {
            CommandProperties analyzerCommandProperties = commandPropertiesService.getCommand(messageAnalyzer.getClass());
            if (analyzerCommandProperties == null
                    || userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), analyzerCommandProperties.getAccessLevel())) {
                parser.executeAsync(botRequest, messageAnalyzer.analyze(botRequest));
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

    public void parseAsync(BotRequest botRequest, Command command) {
        parser.parseAsync(botRequest, command);
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

    private void reportToAdmin(org.telegram.bot.domain.entities.User user, String textMessage) {
        Long adminId = propertiesConfig.getAdminId();
        if (adminId.equals(user.getUserId()) || this.getBotUsername().equals(user.getUsername())) {
            return;
        }

        TextResponse textResponse = spyModeService.generateResponse(user, textMessage);

        this.sendMessage(textResponse);
    }

    public void sendMessage(TextResponse textResponse) {
        parser.executeAsync(textResponse);
    }

    public void sendDocument(FileResponse fileResponse) {
        parser.executeAsync(fileResponse);
    }

    public byte[] getFileFromTelegram(String fileId) {
        try (InputStream inputStream = getInputStreamFromTelegramFile(fileId)) {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            log.error("Failed to get file from telegram", e);
            botStats.incrementErrors(fileId, e, "Failed to get file from telegram");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

    public InputStream getInputStreamFromTelegramFile(String fileId) {
        try {
            return telegramClient.downloadFileAsStream(telegramClient.execute(new GetFile(fileId)).getFilePath());
        } catch (TelegramApiException e) {
            log.error("Failed to get file from telegram", e);
            botStats.incrementErrors(fileId, e, "Failed to get file from telegram");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

    public void sendUploadPhoto(Long chatId) {
        sendAction(chatId, ActionType.UPLOAD_PHOTO);
    }

    public void sendUploadVideo(Long chatId) {
        sendAction(chatId, ActionType.UPLOAD_VIDEO);
    }

    public void sendUploadDocument(BotRequest request) {
        sendAction(request.getMessage().getChatId(), ActionType.UPLOAD_DOCUMENT);
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
            botStats.incrementErrors(sendChatAction, e, "ошибка при отправке Action");
            log.error("Error: cannot send chat action: {}", e.getMessage());
        }
    }

}
