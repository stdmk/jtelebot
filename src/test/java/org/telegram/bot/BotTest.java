package org.telegram.bot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.telegram.bot.commands.Command;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageKind;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.mapper.telegram.request.RequestMapper;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotTest {

    private static final String BOT_USERNAME = "jtelebot";

    @Mock
    private ApplicationContext context;
    @Mock
    private MessageAnalyzerExecutor messageAnalyzerExecutor;
    @Mock
    private BotStats botStats;
    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private RequestMapper requestMapper;
    @Mock
    private CommandPropertiesService commandPropertiesService;
    @Mock
    private UserService userService;
    @Mock
    private UserStatsService userStatsService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private DisableCommandService disableCommandService;
    @Mock
    private LogService logService;
    @Mock
    private Parser parser;
    @Mock
    private TelegramClient telegramClient;

    @InjectMocks
    private Bot bot;

    @Test
    void consumeUpdateWithoutMessageTest() {
        Update update = mock(Update.class);
        BotRequest request = new BotRequest();

        when(requestMapper.toBotRequest(update)).thenReturn(request);

        bot.consume(update);

        verify(botStats).incrementReceivedMessages();
        verify(parser, never()).parseAsync(any(BotRequest.class), any(Command.class));
        verify(messageAnalyzerExecutor, never()).analyzeMessageAsync(any(BotRequest.class), any(AccessLevel.class));
    }

    @Test
    void consumeUpdateWithEditedOldMessageTest() {
        Update update = mock(Update.class);
        BotRequest request = TestUtils.getRequestFromGroup("test");

        Message message = request.getMessage();
        message.setMessageKind(MessageKind.EDIT);
        message.setDateTime(LocalDateTime.now().minusMinutes(1));

        when(requestMapper.toBotRequest(update)).thenReturn(request);

        bot.consume(update);

        verify(botStats).incrementReceivedMessages();
        verify(parser, never()).parseAsync(any(BotRequest.class), any(Command.class));
        verify(messageAnalyzerExecutor, never()).analyzeMessageAsync(any(BotRequest.class), any(AccessLevel.class));
    }

    @Test
    void consumeUpdateFromBannedUserTest() {
        Update update = mock(Update.class);
        BotRequest request = TestUtils.getRequestFromGroup("test");
        Message message = request.getMessage();

        when(requestMapper.toBotRequest(update)).thenReturn(request);
        when(userService.getCurrentAccessLevel(message.getUser().getUserId(), message.getChatId())).thenReturn(AccessLevel.BANNED);

        bot.consume(update);

        verify(botStats).incrementReceivedMessages();
        verify(logService).log(message);
        verify(parser, never()).parseAsync(any(BotRequest.class), any(Command.class));
        verify(messageAnalyzerExecutor, never()).analyzeMessageAsync(any(BotRequest.class), any(AccessLevel.class));
    }

    @Test
    void consumeUpdateWithoutCommandsTest() {
        Update update = mock(Update.class);
        BotRequest request = TestUtils.getRequestFromGroup();
        Message message = request.getMessage();
        message.setText(null);

        when(requestMapper.toBotRequest(update)).thenReturn(request);
        when(userService.getCurrentAccessLevel(message.getUser().getUserId(), message.getChatId())).thenReturn(AccessLevel.NEWCOMER);

        bot.consume(update);

        verify(botStats).incrementReceivedMessages();
        verify(logService).log(message);
        verify(userStatsService).updateEntitiesInfo(message);
        verify(parser, never()).parseAsync(any(BotRequest.class), any(Command.class));
        verify(messageAnalyzerExecutor).analyzeMessageAsync(any(BotRequest.class), any(AccessLevel.class));
    }

    @Test
    void consumeUpdateWithDisabledCommandTest() {
        final String messageText = "test";
        Update update = mock(Update.class);
        BotRequest request = TestUtils.getRequestFromGroup(messageText);
        Message message = request.getMessage();

        when(requestMapper.toBotRequest(update)).thenReturn(request);
        when(userService.getCurrentAccessLevel(message.getUser().getUserId(), message.getChatId())).thenReturn(AccessLevel.NEWCOMER);
        when(propertiesConfig.getTelegramBotUsername()).thenReturn(BOT_USERNAME);
        CommandProperties commandProperties = new CommandProperties();
        when(commandPropertiesService.findCommandInText(messageText, BOT_USERNAME)).thenReturn(commandProperties);
        when(disableCommandService.get(message.getChat(), commandProperties)).thenReturn(new DisableCommand());

        bot.consume(update);

        verify(botStats).incrementReceivedMessages();
        verify(logService).log(message);
        verify(userStatsService).updateEntitiesInfo(message);
        verify(parser, never()).parseAsync(any(BotRequest.class), any(Command.class));
        verify(messageAnalyzerExecutor).analyzeMessageAsync(any(BotRequest.class), any(AccessLevel.class));
    }

    @Test
    void consumeUpdateWithMissingBeanTest() {
        final String messageText = "test";
        Update update = mock(Update.class);
        BotRequest request = TestUtils.getRequestFromGroup(messageText);
        Message message = request.getMessage();

        when(requestMapper.toBotRequest(update)).thenReturn(request);
        when(userService.getCurrentAccessLevel(message.getUser().getUserId(), message.getChatId())).thenReturn(AccessLevel.NEWCOMER);
        when(propertiesConfig.getTelegramBotUsername()).thenReturn(BOT_USERNAME);
        CommandProperties commandProperties = new CommandProperties();
        when(commandPropertiesService.findCommandInText(messageText, BOT_USERNAME)).thenReturn(commandProperties);

        bot.consume(update);

        verify(botStats).incrementReceivedMessages();
        verify(logService).log(message);
        verify(userStatsService).updateEntitiesInfo(message);
        verify(parser, never()).parseAsync(any(BotRequest.class), any(Command.class));
        verify(messageAnalyzerExecutor).analyzeMessageAsync(any(BotRequest.class), any(AccessLevel.class));
    }

    @Test
    void consumeUpdateWithMissingCommandTest() {
        final String messageText = "test";
        Update update = mock(Update.class);
        BotRequest request = TestUtils.getRequestFromGroup(messageText);
        Message message = request.getMessage();

        when(requestMapper.toBotRequest(update)).thenReturn(request);
        AccessLevel userAccessLevel = AccessLevel.NEWCOMER;
        when(userService.getCurrentAccessLevel(message.getUser().getUserId(), message.getChatId())).thenReturn(userAccessLevel);
        when(propertiesConfig.getTelegramBotUsername()).thenReturn(BOT_USERNAME);
        String commandClassName = "commandClass";
        Integer commandAccessLevel = AccessLevel.MODERATOR.getValue();
        CommandProperties commandProperties = new CommandProperties().setClassName(commandClassName).setAccessLevel(commandAccessLevel);
        when(commandPropertiesService.findCommandInText(messageText, BOT_USERNAME)).thenReturn(commandProperties);
        when(context.getBean(commandClassName)).thenThrow(new RuntimeException());

        bot.consume(update);

        verify(botStats).incrementReceivedMessages();
        verify(botStats).incrementErrors(eq(commandProperties), anyString());
        verify(logService).log(message);
        verify(userStatsService).updateEntitiesInfo(message);
        verify(parser, never()).parseAsync(any(BotRequest.class), any(Command.class));
        verify(messageAnalyzerExecutor).analyzeMessageAsync(any(BotRequest.class), any(AccessLevel.class));
    }

    @Test
    void consumeUpdateWithNotEnoughRightsTest() {
        final String messageText = "test";
        Update update = mock(Update.class);
        BotRequest request = TestUtils.getRequestFromGroup(messageText);
        Message message = request.getMessage();

        when(requestMapper.toBotRequest(update)).thenReturn(request);
        AccessLevel userAccessLevel = AccessLevel.NEWCOMER;
        when(userService.getCurrentAccessLevel(message.getUser().getUserId(), message.getChatId())).thenReturn(userAccessLevel);
        when(propertiesConfig.getTelegramBotUsername()).thenReturn(BOT_USERNAME);
        String commandClassName = "commandClass";
        Integer commandAccessLevel = AccessLevel.MODERATOR.getValue();
        CommandProperties commandProperties = new CommandProperties().setClassName(commandClassName).setAccessLevel(commandAccessLevel);
        when(commandPropertiesService.findCommandInText(messageText, BOT_USERNAME)).thenReturn(commandProperties);
        Command command = mock(Command.class);
        when(context.getBean(commandClassName)).thenReturn(command);
        when(userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), commandAccessLevel)).thenReturn(false);

        bot.consume(update);

        verify(botStats).incrementReceivedMessages();
        verify(logService).log(message);
        verify(userStatsService).updateEntitiesInfo(message);
        verify(parser, never()).parseAsync(any(BotRequest.class), any(Command.class));
        verify(messageAnalyzerExecutor).analyzeMessageAsync(any(BotRequest.class), any(AccessLevel.class));
    }

    @Test
    void consumeUpdateTest() {
        final String messageText = "test";
        Update update = mock(Update.class);
        BotRequest request = TestUtils.getRequestFromGroup(messageText);
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        when(requestMapper.toBotRequest(update)).thenReturn(request);
        AccessLevel userAccessLevel = AccessLevel.NEWCOMER;
        when(userService.getCurrentAccessLevel(user.getUserId(), message.getChatId())).thenReturn(userAccessLevel);
        CommandWaiting commandWaiting = new CommandWaiting().setCommandName(messageText);
        when(commandWaitingService.get(chat, user)).thenReturn(commandWaiting);
        String commandClassName = "commandClass";
        Integer commandAccessLevel = AccessLevel.NEWCOMER.getValue();
        CommandProperties commandProperties = new CommandProperties().setClassName(commandClassName).setAccessLevel(commandAccessLevel);
        when(commandPropertiesService.getCommand(messageText)).thenReturn(commandProperties);
        Command command = mock(Command.class);
        when(context.getBean(commandClassName)).thenReturn(command);
        when(userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), commandAccessLevel)).thenReturn(true);

        bot.consume(update);

        verify(botStats).incrementReceivedMessages();
        verify(logService).log(message);
        verify(userStatsService).updateEntitiesInfo(message);
        verify(userStatsService).incrementUserStatsCommands(chat, user, commandProperties);
        verify(parser).parseAsync(any(BotRequest.class), any(Command.class));
        verify(messageAnalyzerExecutor).analyzeMessageAsync(any(BotRequest.class), any(AccessLevel.class));
    }

    @Test
    void processRequestWithoutAnalyzeTest() {
        final String messageText = "test";
        BotRequest request = TestUtils.getRequestFromGroup(messageText);
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        AccessLevel userAccessLevel = AccessLevel.NEWCOMER;
        when(userService.getCurrentAccessLevel(user.getUserId(), message.getChatId())).thenReturn(userAccessLevel);
        CommandWaiting commandWaiting = new CommandWaiting().setCommandName(messageText);
        when(commandWaitingService.get(chat, user)).thenReturn(commandWaiting);
        String commandClassName = "commandClass";
        Integer commandAccessLevel = AccessLevel.NEWCOMER.getValue();
        CommandProperties commandProperties = new CommandProperties().setClassName(commandClassName).setAccessLevel(commandAccessLevel);
        when(commandPropertiesService.getCommand(messageText)).thenReturn(commandProperties);
        Command command = mock(Command.class);
        when(context.getBean(commandClassName)).thenReturn(command);
        when(userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), commandAccessLevel)).thenReturn(true);

        bot.processRequestWithoutAnalyze(request);

        verify(userStatsService).incrementUserStatsCommands(chat, user, commandProperties);
        verify(parser).parseAsync(any(BotRequest.class), any(Command.class));
        verify(messageAnalyzerExecutor, never()).analyzeMessageAsync(any(BotRequest.class), any(AccessLevel.class));
    }

    @Test
    void processRequestTest() {
        final String messageText = "test";
        BotRequest request = TestUtils.getRequestFromGroup(messageText);
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        AccessLevel userAccessLevel = AccessLevel.NEWCOMER;
        when(userService.getCurrentAccessLevel(user.getUserId(), message.getChatId())).thenReturn(userAccessLevel);
        CommandWaiting commandWaiting = new CommandWaiting().setCommandName(messageText);
        when(commandWaitingService.get(chat, user)).thenReturn(commandWaiting);
        String commandClassName = "commandClass";
        Integer commandAccessLevel = AccessLevel.NEWCOMER.getValue();
        CommandProperties commandProperties = new CommandProperties().setClassName(commandClassName).setAccessLevel(commandAccessLevel);
        when(commandPropertiesService.getCommand(messageText)).thenReturn(commandProperties);
        Command command = mock(Command.class);
        when(context.getBean(commandClassName)).thenReturn(command);
        when(userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), commandAccessLevel)).thenReturn(true);

        bot.processRequest(request);

        verify(userStatsService).incrementUserStatsCommands(chat, user, commandProperties);
        verify(parser).parseAsync(any(BotRequest.class), any(Command.class));
        verify(messageAnalyzerExecutor).analyzeMessageAsync(any(BotRequest.class), any(AccessLevel.class));
    }

    @Test
    void getBotTokenTest() {
        final String expectedBotToken = "token";
        when(propertiesConfig.getTelegramBotApiToken()).thenReturn(expectedBotToken);
        String actualBotToken = bot.getBotToken();
        assertEquals(expectedBotToken, actualBotToken);
    }

    @Test
    void getUpdatesConsumerTest() {
        LongPollingUpdateConsumer updatesConsumer = bot.getUpdatesConsumer();
        assertEquals(bot, updatesConsumer);
    }

    @Test
    void getBotUsernameFromPropertiesConfigTest() {
        final String expectedBotUsername = "bot";
        when(propertiesConfig.getTelegramBotUsername()).thenReturn(expectedBotUsername);
        String actualBotUsername = bot.getBotUsername();
        assertEquals(expectedBotUsername, actualBotUsername);
    }

    @Test
    void getBotUsernameFromApiTest() throws TelegramApiException {
        final String expectedBotUsername = "bot";

        org.telegram.telegrambots.meta.api.objects.User user = new org.telegram.telegrambots.meta.api.objects.User(1L, "firstName", true);
        user.setUserName(expectedBotUsername);
        when(telegramClient.execute(any(GetMe.class))).thenReturn(user);

        String actualBotUsername = bot.getBotUsername();

        assertEquals(expectedBotUsername, actualBotUsername);
        verify(propertiesConfig).setTelegramBotUsername(expectedBotUsername);
    }

    @Test
    void getBotUsernameDefaultTest() throws TelegramApiException {
        final String expectedBotUsername = "jtelebot";

        when(telegramClient.execute(any(GetMe.class))).thenThrow(new TelegramApiException());

        String actualBotUsername = bot.getBotUsername();

        assertEquals(expectedBotUsername, actualBotUsername);
        verify(propertiesConfig, never()).setTelegramBotUsername(expectedBotUsername);
    }

    @Test
    void sendMessageTest() {
        TextResponse textResponse = new TextResponse();
        bot.sendMessage(textResponse);
        verify(parser).executeAsync(textResponse);
    }

    @Test
    void sendDocumentTest() {
        FileResponse fileResponse = new FileResponse();
        bot.sendDocument(fileResponse);
        verify(parser).executeAsync(fileResponse);
    }

    @Test
    void sendUploadPhotoTest() throws TelegramApiException {
        final Long chatId = 123L;

        bot.sendUploadPhoto(chatId);

        ArgumentCaptor<SendChatAction> sendChatActionCaptor = ArgumentCaptor.forClass(SendChatAction.class);
        verify(telegramClient).execute(sendChatActionCaptor.capture());

        SendChatAction sendChatAction = sendChatActionCaptor.getValue();
        assertEquals(chatId.toString(), sendChatAction.getChatId());
        assertEquals(ActionType.UPLOAD_PHOTO.toString(), sendChatAction.getAction());
    }

    @Test
    void sendUploadVideoTest() throws TelegramApiException {
        final Long chatId = 123L;

        bot.sendUploadVideo(chatId);

        ArgumentCaptor<SendChatAction> sendChatActionCaptor = ArgumentCaptor.forClass(SendChatAction.class);
        verify(telegramClient).execute(sendChatActionCaptor.capture());

        SendChatAction sendChatAction = sendChatActionCaptor.getValue();
        assertEquals(chatId.toString(), sendChatAction.getChatId());
        assertEquals(ActionType.UPLOAD_VIDEO.toString(), sendChatAction.getAction());
    }

    @Test
    void sendUploadDocumentTest() throws TelegramApiException {
        final Long chatId = 123L;

        bot.sendUploadDocument(chatId);

        ArgumentCaptor<SendChatAction> sendChatActionCaptor = ArgumentCaptor.forClass(SendChatAction.class);
        verify(telegramClient).execute(sendChatActionCaptor.capture());

        SendChatAction sendChatAction = sendChatActionCaptor.getValue();
        assertEquals(chatId.toString(), sendChatAction.getChatId());
        assertEquals(ActionType.UPLOAD_DOCUMENT.toString(), sendChatAction.getAction());
    }

    @Test
    void sendTypingTest() throws TelegramApiException {
        final Long chatId = 123L;

        bot.sendTyping(chatId);

        ArgumentCaptor<SendChatAction> sendChatActionCaptor = ArgumentCaptor.forClass(SendChatAction.class);
        verify(telegramClient).execute(sendChatActionCaptor.capture());

        SendChatAction sendChatAction = sendChatActionCaptor.getValue();
        assertEquals(chatId.toString(), sendChatAction.getChatId());
        assertEquals(ActionType.TYPING.toString(), sendChatAction.getAction());
    }

    @Test
    void sendLocationTest() throws TelegramApiException {
        final Long chatId = 123L;

        bot.sendLocation(chatId);

        ArgumentCaptor<SendChatAction> sendChatActionCaptor = ArgumentCaptor.forClass(SendChatAction.class);
        verify(telegramClient).execute(sendChatActionCaptor.capture());

        SendChatAction sendChatAction = sendChatActionCaptor.getValue();
        assertEquals(chatId.toString(), sendChatAction.getChatId());
        assertEquals(ActionType.FIND_LOCATION.toString(), sendChatAction.getAction());
    }

    @Test
    void failedToSendActionTest() throws TelegramApiException {
        final Long chatId = 123L;
        ActionType action = ActionType.TYPING;

        TelegramApiException telegramApiException = new TelegramApiException();
        when(telegramClient.execute(any(SendChatAction.class))).thenThrow(telegramApiException);
        bot.sendAction(chatId, action);

        ArgumentCaptor<SendChatAction> sendChatActionCaptor1 = ArgumentCaptor.forClass(SendChatAction.class);
        verify(botStats).incrementErrors(sendChatActionCaptor1.capture(), eq(telegramApiException), eq("failed to send Action"));
        ArgumentCaptor<SendChatAction> sendChatActionCaptor2 = ArgumentCaptor.forClass(SendChatAction.class);
        verify(telegramClient).execute(sendChatActionCaptor2.capture());

        SendChatAction sendChatAction1 = sendChatActionCaptor1.getValue();
        SendChatAction sendChatAction2 = sendChatActionCaptor2.getValue();
        assertEquals(sendChatAction1, sendChatAction2);

        assertEquals(chatId.toString(), sendChatAction1.getChatId());
        assertEquals(action.toString(), sendChatAction1.getAction());
    }

}