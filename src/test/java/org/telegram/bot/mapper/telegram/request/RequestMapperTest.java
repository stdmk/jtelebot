package org.telegram.bot.mapper.telegram.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.bot.domain.model.request.MessageKind;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.reactions.MessageReactionUpdated;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestMapperTest {

    private static final String MESSAGE_TEXT = "text";
    private static final Integer MESSAGE_ID = 12345678;
    private static final org.telegram.telegrambots.meta.api.objects.chat.Chat CHAT = new org.telegram.telegrambots.meta.api.objects.chat.Chat(-1L, "type");
    private static final org.telegram.telegrambots.meta.api.objects.User USER = new org.telegram.telegrambots.meta.api.objects.User(1L, "name", false);
    private static final LocalDateTime MESSAGE_DATE = LocalDateTime.of(2000, 1, 1, 0, 1, 3);
    private static final LocalDateTime MESSAGE_EDIT_DATE = MESSAGE_DATE.plusMinutes(5);
    private static final MessageContentType MESSAGE_CONTENT_TYPE = MessageContentType.TEXT;
    private static final MessageKind MESSAGE_KIND = MessageKind.COMMON;

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private RequestMapper requestMapper;

    @Test
    void toBotRequestReactionTest() {
        Update update = new Update();
        MessageReactionUpdated messageReactionUpdated = new MessageReactionUpdated();
        update.setMessageReaction(messageReactionUpdated);

        Message message = new Message();
        when(messageMapper.toMessage(messageReactionUpdated)).thenReturn(message);

        BotRequest botRequest = requestMapper.toBotRequest(update);

        assertEquals(message, botRequest.getMessage());
    }

    @Test
    void toBotRequestWithoutMessageTest() {
        Update update = new Update();

        BotRequest botRequest = requestMapper.toBotRequest(update);

        assertNotNull(botRequest);
        assertNull(botRequest.getMessage());
    }

    @ParameterizedTest
    @MethodSource("provideUpdates")
    void toBotRequestTest(Update update, org.telegram.telegrambots.meta.api.objects.message.Message telegramMessage, MessageKind messageKind) {
        Message expectedMessage = new Message();

        when(messageMapper.toMessage(telegramMessage, USER, MESSAGE_TEXT, messageKind)).thenReturn(expectedMessage);

        BotRequest botRequest = requestMapper.toBotRequest(update);

        assertNotNull(botRequest);
        assertEquals(expectedMessage, botRequest.getMessage());
    }

    private static Stream<Arguments> provideUpdates() {
        org.telegram.telegrambots.meta.api.objects.message.Message messageWithoutFrom = buildMessage(null);
        org.telegram.telegrambots.meta.api.objects.message.Message message = buildMessage(USER);
        org.telegram.telegrambots.meta.api.objects.message.Message messageWithCaption = buildMessage(USER);
        messageWithCaption.setText(null);
        messageWithCaption.setCaption(MESSAGE_TEXT);

        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setMessage(messageWithoutFrom);
        callbackQuery.setFrom(USER);
        callbackQuery.setData(MESSAGE_TEXT);
        Update callbackUpdate = new Update();
        callbackUpdate.setCallbackQuery(callbackQuery);

        Update editMessageUpdate = new Update();
        editMessageUpdate.setEditedMessage(message);

        Update updateWithPhoto = new Update();
        updateWithPhoto.setMessage(messageWithCaption);

        Update update2 = new Update();
        update2.setMessage(messageWithCaption);

        Update update = new Update();
        update.setMessage(message);

        return Stream.of(
                Arguments.of(callbackUpdate, messageWithoutFrom, MessageKind.CALLBACK),
                Arguments.of(editMessageUpdate, message, MessageKind.EDIT),
                Arguments.of(update2, messageWithCaption, MessageKind.COMMON),
                Arguments.of(update, message, MessageKind.COMMON)
        );
    }

    private static org.telegram.telegrambots.meta.api.objects.message.Message buildMessage(org.telegram.telegrambots.meta.api.objects.User user) {
        org.telegram.telegrambots.meta.api.objects.message.Message message = new org.telegram.telegrambots.meta.api.objects.message.Message();

        message.setMessageId(MESSAGE_ID);
        message.setChat(CHAT);
        message.setFrom(user);
        message.setText(MESSAGE_TEXT);
        message.setDate((int) MESSAGE_DATE.toEpochSecond(ZoneOffset.UTC));
        message.setEditDate((int) MESSAGE_EDIT_DATE.toEpochSecond(ZoneOffset.UTC));

        return message;
    }

}