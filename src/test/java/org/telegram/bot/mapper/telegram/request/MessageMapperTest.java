package org.telegram.bot.mapper.telegram.request;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.*;
import org.telegram.telegrambots.meta.api.objects.reactions.MessageReactionUpdated;
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionType;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageMapperTest {

    private static final String MESSAGE_TEXT = "text";
    private static final Integer MESSAGE_ID = 12345678;
    private static final org.telegram.telegrambots.meta.api.objects.chat.Chat CHAT = new org.telegram.telegrambots.meta.api.objects.chat.Chat(-1L, "type");
    private static final org.telegram.telegrambots.meta.api.objects.User USER = new org.telegram.telegrambots.meta.api.objects.User(1L, "name", false);
    private static final LocalDateTime MESSAGE_DATE = LocalDateTime.of(2000, 1, 1, 0, 1, 3);
    private static final LocalDateTime MESSAGE_EDIT_DATE = MESSAGE_DATE.plusMinutes(5);
    private static final MessageContentType MESSAGE_CONTENT_TYPE = MessageContentType.TEXT;
    private static final MessageKind MESSAGE_KIND = MessageKind.COMMON;

    @Mock
    private ChatMapper chatMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AttachmentMapper attachmentMapper;
    @Mock
    private ReactionMapper reactionMapper;

    @InjectMocks
    private MessageMapper messageMapper;

    @Test
    void toMessageTest() {
        org.telegram.telegrambots.meta.api.objects.message.Message message = new org.telegram.telegrambots.meta.api.objects.message.Message();

        message.setMessageId(MESSAGE_ID);
        message.setChat(CHAT);
        message.setFrom(USER);
        message.setText(MESSAGE_TEXT);
        message.setDate((int) MESSAGE_DATE.toEpochSecond(ZoneOffset.UTC));
        message.setEditDate((int) MESSAGE_EDIT_DATE.toEpochSecond(ZoneOffset.UTC));

        List<Attachment> attachments = new ArrayList<>();
        when(attachmentMapper.toAttachments(message)).thenReturn(Pair.of(MESSAGE_CONTENT_TYPE, attachments));
        Chat expectedChat = new Chat();
        when(chatMapper.toChat(CHAT)).thenReturn(expectedChat);
        User expectedUser = new User();
        when(userMapper.toUser(USER)).thenReturn(expectedUser);

        Message actual = messageMapper.toMessage(message, USER, MESSAGE_TEXT, MESSAGE_KIND);

        assertEquals(expectedChat, actual.getChat());
        assertEquals(expectedUser, actual.getUser());
        assertEquals(MESSAGE_ID, actual.getMessageId());
        assertNull(actual.getReplyToMessage());
        assertEquals(MESSAGE_TEXT, actual.getText());
        assertEquals(MESSAGE_KIND, actual.getMessageKind());
        assertEquals(MESSAGE_CONTENT_TYPE, actual.getMessageContentType());
        assertEquals(attachments, actual.getAttachments());
    }

    @Test
    void toMessageWithReplyMessageTest() {
        org.telegram.telegrambots.meta.api.objects.message.Message replyMessage = new org.telegram.telegrambots.meta.api.objects.message.Message();

        replyMessage.setMessageId(MESSAGE_ID);
        replyMessage.setChat(CHAT);
        replyMessage.setFrom(USER);
        replyMessage.setText(MESSAGE_TEXT);
        replyMessage.setDate((int) MESSAGE_DATE.toEpochSecond(ZoneOffset.UTC));
        replyMessage.setEditDate((int) MESSAGE_EDIT_DATE.toEpochSecond(ZoneOffset.UTC));

        org.telegram.telegrambots.meta.api.objects.message.Message message = new org.telegram.telegrambots.meta.api.objects.message.Message();

        message.setMessageId(MESSAGE_ID);
        message.setChat(CHAT);
        message.setFrom(USER);
        message.setText(MESSAGE_TEXT);
        message.setDate((int) MESSAGE_DATE.toEpochSecond(ZoneOffset.UTC));
        message.setEditDate((int) MESSAGE_EDIT_DATE.toEpochSecond(ZoneOffset.UTC));
        message.setReplyToMessage(replyMessage);

        List<Attachment> attachments = new ArrayList<>();
        when(attachmentMapper.toAttachments(message)).thenReturn(Pair.of(MESSAGE_CONTENT_TYPE, attachments));
        when(attachmentMapper.toAttachments(replyMessage)).thenReturn(Pair.of(MESSAGE_CONTENT_TYPE, attachments));
        Chat expectedChat = new Chat();
        when(chatMapper.toChat(CHAT)).thenReturn(expectedChat);
        User expectedUser = new User();
        when(userMapper.toUser(USER)).thenReturn(expectedUser);

        Message actual = messageMapper.toMessage(message, USER, MESSAGE_TEXT, MESSAGE_KIND);

        assertEquals(expectedChat, actual.getChat());
        assertEquals(expectedUser, actual.getUser());
        assertEquals(MESSAGE_ID, actual.getMessageId());
        assertEquals(MESSAGE_TEXT, actual.getText());
        assertEquals(MESSAGE_KIND, actual.getMessageKind());
        assertEquals(MESSAGE_CONTENT_TYPE, actual.getMessageContentType());
        assertEquals(attachments, actual.getAttachments());

        Message replyToMessage = actual.getReplyToMessage();
        assertEquals(expectedChat, replyToMessage.getChat());
        assertEquals(expectedUser, replyToMessage.getUser());
        assertEquals(MESSAGE_ID, replyToMessage.getMessageId());
        assertEquals(MESSAGE_TEXT, replyToMessage.getText());
        assertEquals(MESSAGE_KIND, replyToMessage.getMessageKind());
        assertEquals(MESSAGE_CONTENT_TYPE, replyToMessage.getMessageContentType());
        assertEquals(attachments, replyToMessage.getAttachments());
    }

    @Test
    void toMessageFromReactionTest() {
        MessageReactionUpdated reaction = new MessageReactionUpdated();
        List<ReactionType> newReactions = new ArrayList<>();
        List<ReactionType> oldReactions = new ArrayList<>();
        reaction.setNewReaction(newReactions);
        reaction.setOldReaction(oldReactions);

        reaction.setMessageId(MESSAGE_ID);
        reaction.setChat(CHAT);
        reaction.setUser(USER);
        reaction.setDate((int) MESSAGE_DATE.toEpochSecond(ZoneOffset.UTC));

        Chat expectedChat = new Chat();
        when(chatMapper.toChat(CHAT)).thenReturn(expectedChat);
        User expectedUser = new User();
        when(userMapper.toUser(USER)).thenReturn(expectedUser);
        Reactions reactions = new Reactions();
        when(reactionMapper.toReactions(oldReactions, newReactions)).thenReturn(reactions);

        Message actual = messageMapper.toMessage(reaction);

        assertEquals(expectedChat, actual.getChat());
        assertEquals(expectedUser, actual.getUser());
        assertEquals(MESSAGE_ID, actual.getMessageId());
        assertEquals(MessageKind.COMMON, actual.getMessageKind());
        assertEquals(MessageContentType.REACTION, actual.getMessageContentType());
        assertEquals(reactions, actual.getReactions());
    }

}