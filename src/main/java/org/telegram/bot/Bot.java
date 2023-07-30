package org.telegram.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

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
    private final Parser parser;

    public Bot(@Lazy List<TextAnalyzer> textAnalyzerList,
               ApplicationContext context,
               BotStats botStats,
               PropertiesConfig propertiesConfig,
               CommandPropertiesService commandPropertiesService,
               UserService userService, UserStatsService userStatsService,
               CommandWaitingService commandWaitingService,
               DisableCommandService disableCommandService,
               SpyModeService spyModeService,
               @Value("${telegramBotApiToken}") String botToken, Parser parser) {
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
        this.parser = parser;
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

        textAnalyzerList.forEach(textAnalyzer -> textAnalyzer.analyze((CommandParent<?>) textAnalyzer, update));

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

        if (command == null) {
            return;
        }

        if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), commandProperties.getAccessLevel())) {
            userStatsService.incrementUserStatsCommands(chatEntity, userEntity, commandProperties);
            parseAsync(update, command);
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

    public void parseAsync(Update update, CommandParent<?> command) {
        parser.parseAsync(update, command)
                .exceptionally(e -> handleException(update, e))
                .thenAccept(method -> executeMethod(update, method));
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

    private PartialBotApiMethod<?> handleException(Update update, Throwable e) {
        Throwable cause = e.getCause();
        Message message = getMessage(update);

        if (cause instanceof BotException) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setText(cause.getMessage());

            return sendMessage;
        }

        botStats.incrementErrors(update, cause, "неожиданная верхнеуровневая ошибка");
        log.error("Unexpected error: ", cause);
        return null;
    }

    public void sendTyping(Update update) {
        sendTyping(getMessage(update).getChatId());
    }

    public void sendUploadPhoto(Long chatId) {
        sendAction(chatId, ActionType.UPLOADPHOTO);
    }

    public void sendUploadVideo(Long chatId) {
        sendAction(chatId, ActionType.UPLOADVIDEO);
    }

    public void sendUploadDocument(Update update) {
        sendAction(getMessage(update).getChatId(), ActionType.UPLOADDOCUMENT);
    }

    public void sendUploadDocument(Long chatId) {
        sendAction(chatId, ActionType.UPLOADDOCUMENT);
    }

    public void sendTyping(Long chatId) {
        sendAction(chatId, ActionType.TYPING);
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

    public void executeMethod(Update update, PartialBotApiMethod<?> method) {
        if (method == null) {
            return;
        }

        Message message = getMessage(update);

        try {
            if (method instanceof SendMessage) {
                SendMessage sendMessage = (SendMessage) method;
                log.info("To " + message.getChatId() + ": " + sendMessage.getText());
                execute(sendMessage);
            } else if (method instanceof SendPhoto) {
                SendPhoto sendPhoto = (SendPhoto) method;
                log.info("To " + message.getChatId() + ": sending photo " + sendPhoto.getCaption());
                try {
                    execute(sendPhoto);
                } catch (TelegramApiException e) {
                    tryToDeliverTheMessage(sendPhoto);
                }
            } else if (method instanceof SendMediaGroup) {
                SendMediaGroup sendMediaGroup = (SendMediaGroup) method;
                log.info("To " + message.getChatId() + ": sending photos " + sendMediaGroup);
                try {
                    execute(sendMediaGroup);
                } catch (TelegramApiException e) {
                    tryToSendOnePhoto(sendMediaGroup);
                }
            } else if (method instanceof SendVideo) {
                SendVideo sendVideo = (SendVideo) method;
                log.info("To " + message.getChatId() + ": " + sendVideo.getCaption());
                execute(sendVideo);
            } else if (method instanceof EditMessageText) {
                EditMessageText editMessageText = (EditMessageText) method;
                log.info("To " + message.getChatId() + ": edited message " + editMessageText.getText());
                execute(editMessageText);
            } else if (method instanceof SendDocument) {
                SendDocument sendDocument = (SendDocument) method;
                log.info("To " + message.getChatId() + ": sending document " + sendDocument.getCaption());
                execute(sendDocument);
            } else if (method instanceof DeleteMessage) {
                DeleteMessage deleteMessage = (DeleteMessage) method;
                log.info("Deleting message {}", deleteMessage.getMessageId());
                execute(deleteMessage);
            }
        } catch (TelegramApiRequestException e) {
            botStats.incrementErrors(update, method, e, "ошибка при отправке ответа");
            log.error("Error: cannot send response: {}", e.getApiResponse());
        } catch (TelegramApiException e) {
            botStats.incrementErrors(update, method, e, "ошибка при отправке ответа");
            log.error("Error: cannot send response: {}", e.getMessage());
        } catch (BotException botException) {
            try {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setReplyToMessageId(message.getMessageId());
                sendMessage.setChatId(message.getChatId().toString());
                sendMessage.setText(botException.getMessage());

                execute(sendMessage);
            } catch (TelegramApiException e) {
                log.error("Error: cannot send response: {}", e.getMessage());
            }
        } catch (Exception e) {
            botStats.incrementErrors(update, method, e, "неожиданная верхнеуровневая ошибка");
            log.error("Unexpected error: ", e);
        }

        botStats.incrementCommandsProcessed();
    }

    private Message getMessage(Update update) {
        Message message = update.getMessage();
        if (message == null) {
            message = update.getEditedMessage();
            if (message == null) {
                message = update.getCallbackQuery().getMessage();
            }
        }

        return message;
    }

    private void tryToSendOnePhoto(SendMediaGroup sendMediaGroup) {
        StringBuilder buf = new StringBuilder("Остальные картинки: \n");
        sendMediaGroup.getMedias().stream().skip(1).forEach(inputMedia -> buf.append(inputMedia.getCaption()).append("\n"));

        InputMedia inputMedia = sendMediaGroup.getMedias().get(0);
        InputFile inputFile = new InputFile();
        inputFile.setMedia(inputMedia.getMedia());

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(inputFile);
        sendPhoto.setReplyToMessageId(sendMediaGroup.getReplyToMessageId());
        sendPhoto.setChatId(sendMediaGroup.getChatId());
        sendPhoto.setCaption(buf.toString());

        try {
            execute(sendPhoto);
        } catch (TelegramApiException telegramApiException) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(sendMediaGroup.getChatId());
            sendMessage.setReplyToMessageId(sendMediaGroup.getReplyToMessageId());
            sendMessage.setText("Не удалось загрузить картинку по адресу: " + inputMedia.getMedia() + "\n" + buf);

            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void tryToDeliverTheMessage(SendPhoto sendPhoto) throws TelegramApiException {
        String imageUrl = sendPhoto.getPhoto().getAttachName();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyToMessageId(sendPhoto.getReplyToMessageId());
        sendMessage.setChatId(sendPhoto.getChatId());
        sendMessage.setText("Не удалось отправить картинку с адреса: " + imageUrl + "\n" + sendPhoto.getCaption());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();

        execute(sendMessage);
    }
}
