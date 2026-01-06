package org.telegram.bot.mapper.telegram.request;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserEmail;
import org.telegram.bot.domain.model.request.*;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.mapper.bot.request.*;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.UserEmailService;
import org.telegram.telegrambots.meta.api.objects.reactions.MessageReactionUpdated;
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionType;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageMapperTest {

    private static final String MESSAGE_TEXT = "text";
    private static final Integer MESSAGE_ID = 12345678;
    private static final org.telegram.telegrambots.meta.api.objects.chat.Chat CHAT = new org.telegram.telegrambots.meta.api.objects.chat.Chat(-1L, "type");
    private static final org.telegram.telegrambots.meta.api.objects.User USER = new org.telegram.telegrambots.meta.api.objects.User(1L, "name", false);
    private static final LocalDateTime MESSAGE_DATE_TIME = LocalDateTime.of(2000, 1, 1, 0, 1, 3);
    private static final LocalDateTime MESSAGE_EDIT_DATE_TIME = MESSAGE_DATE_TIME.plusMinutes(5);
    private static final Date MESSAGE_DATE = Date.from(MESSAGE_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
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
    @Mock
    private UserEmailService userEmailService;
    @Mock
    private BotStats botStats;

    @InjectMocks
    private MessageMapper messageMapper;

    @Captor
    private ArgumentCaptor<List<String>> emailAddressesCaptor;

    @Test
    void toMessageTest() {
        org.telegram.telegrambots.meta.api.objects.message.Message message = new org.telegram.telegrambots.meta.api.objects.message.Message();

        message.setMessageId(MESSAGE_ID);
        message.setChat(CHAT);
        message.setFrom(USER);
        message.setText(MESSAGE_TEXT);
        message.setDate((int) MESSAGE_DATE_TIME.toEpochSecond(ZoneOffset.UTC));
        message.setEditDate((int) MESSAGE_EDIT_DATE_TIME.toEpochSecond(ZoneOffset.UTC));

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
        replyMessage.setDate((int) MESSAGE_DATE_TIME.toEpochSecond(ZoneOffset.UTC));
        replyMessage.setEditDate((int) MESSAGE_EDIT_DATE_TIME.toEpochSecond(ZoneOffset.UTC));

        org.telegram.telegrambots.meta.api.objects.message.Message message = new org.telegram.telegrambots.meta.api.objects.message.Message();

        message.setMessageId(MESSAGE_ID);
        message.setChat(CHAT);
        message.setFrom(USER);
        message.setText(MESSAGE_TEXT);
        message.setDate((int) MESSAGE_DATE_TIME.toEpochSecond(ZoneOffset.UTC));
        message.setEditDate((int) MESSAGE_EDIT_DATE_TIME.toEpochSecond(ZoneOffset.UTC));
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
        reaction.setDate((int) MESSAGE_DATE_TIME.toEpochSecond(ZoneOffset.UTC));

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

    @Test
    void toMessageFromEmailMessageWithUnknownAddressTest() throws MessagingException {
        javax.mail.Message emailMessage = mock(javax.mail.Message.class);
        Address[] from = new Address[]{new InternetAddress("email@example.com")};
        when(emailMessage.getFrom()).thenReturn(from);

        Message message = messageMapper.toMessage(emailMessage);

        assertNull(message);
    }

    @Test
    void toMessageFromEmailMessageWithUnknownTypeOfMessageTest() throws MessagingException, IOException {
        final String emailAddress = "email@example.com";

        javax.mail.Message emailMessage = mock(javax.mail.Message.class);
        Address[] from = new Address[]{new InternetAddress(emailAddress)};
        when(emailMessage.getFrom()).thenReturn(from);
        when(emailMessage.getContent()).thenReturn(new Object());
        UserEmail userEmail = new UserEmail().setUser(new User());
        when(userEmailService.get(anyList())).thenReturn(userEmail);

        Message message = messageMapper.toMessage(emailMessage);

        assertNull(message);

        verify(userEmailService).get(emailAddressesCaptor.capture());
        List<String> actualEmailAddresses = emailAddressesCaptor.getValue();
        assertEquals(1, actualEmailAddresses.size());
        assertEquals(emailAddress, actualEmailAddresses.get(0));

        verify(botStats).incrementErrors(emailMessage, "Unexpected type of email message: " + emailMessage.getClass().getName());
    }

    @Test
    void toMessageFromEmailMessageWithExceptionTest() throws MessagingException, IOException {
        final String emailAddress = "email@example.com";
        final String error = "error";

        javax.mail.Message emailMessage = mock(javax.mail.Message.class);
        Address[] from = new Address[]{new InternetAddress(emailAddress)};
        when(emailMessage.getFrom()).thenReturn(from);
        UserEmail userEmail = new UserEmail().setUser(new User());
        when(userEmailService.get(anyList())).thenReturn(userEmail);
        MessagingException exception = new MessagingException(error);
        when(emailMessage.getContent()).thenThrow(exception);

        Message message = messageMapper.toMessage(emailMessage);

        assertNull(message);

        verify(botStats).incrementErrors(emailMessage, exception, "Failed to map email-message: " + error);
    }

    @Test
    void toMessageFromEmailMessageWithOnlyTextTest() throws MessagingException, IOException {
        final String emailAddress = "email@example.com";
        final String messageText = "text";

        javax.mail.Message emailMessage = mock(MimeMessage.class);
        Address[] from = new Address[]{new InternetAddress(emailAddress)};
        when(emailMessage.getFrom()).thenReturn(from);
        when(emailMessage.getContent()).thenReturn(messageText);
        User user = TestUtils.getUser();
        UserEmail userEmail = new UserEmail().setUser(user);
        when(userEmailService.get(anyList())).thenReturn(userEmail);

        Message message = messageMapper.toMessage(emailMessage);

        assertNotNull(message);

        Chat chat = message.getChat();
        assertNotNull(chat);
        assertEquals(user.getUserId(), chat.getChatId());
        assertNull(message.getMessageId());
        assertEquals(messageText, message.getText());
        assertEquals(MessageKind.COMMON, message.getMessageKind());
        assertEquals(MessageContentType.TEXT, message.getMessageContentType());
        assertTrue(message.getAttachments().isEmpty());
        assertNull(message.getReplyToMessage());

        verify(userEmailService).get(emailAddressesCaptor.capture());
        List<String> actualEmailAddresses = emailAddressesCaptor.getValue();
        assertEquals(1, actualEmailAddresses.size());
        assertEquals(emailAddress, actualEmailAddresses.get(0));
    }

    @Test
    void toMessageFromEmailMessageTest() throws MessagingException, IOException {
        final String emailAddress = "email@example.com";
        final String messageText = "text";
        final byte[] attachmentBytes = "content".getBytes(StandardCharsets.UTF_8);
        final int contentPartsCount = 2;

        javax.mail.Message emailMessage = mock(MimeMessage.class);
        Address[] from = new Address[]{new InternetAddress(emailAddress)};
        when(emailMessage.getFrom()).thenReturn(from);
        Multipart content = mock(Multipart.class);
        when(content.getCount()).thenReturn(contentPartsCount);
        BodyPart part1 = mock(BodyPart.class);
        when(part1.isMimeType("multipart/*")).thenReturn(true);
        Multipart part10 = mock(Multipart.class);
        when(part10.getCount()).thenReturn(1);
        BodyPart attachmentPart = mock(BodyPart.class);
        when(attachmentPart.isMimeType(anyString())).thenReturn(false);
        when(attachmentPart.getDisposition()).thenReturn(Part.ATTACHMENT);
        InputStream attachmentInputStream = mock(InputStream.class);
        when(attachmentInputStream.readAllBytes()).thenReturn(attachmentBytes);
        when(attachmentPart.getInputStream()).thenReturn(attachmentInputStream);
        when(attachmentPart.getFileName()).thenReturn("filename.txt");
        when(attachmentPart.getSize()).thenReturn(attachmentBytes.length);
        when(attachmentPart.getContentType()).thenReturn("application");
        when(part10.getBodyPart(0)).thenReturn(attachmentPart);
        when(part1.getContent()).thenReturn(part10);
        BodyPart part2 = mock(BodyPart.class);
        when(part2.isMimeType("text/html")).thenReturn(true);
        when(part2.isMimeType("multipart/*")).thenReturn(false);
        when(part2.isMimeType("text/plain")).thenReturn(false);
        when(part2.getContent()).thenReturn(messageText);
        when(emailMessage.getContent()).thenReturn(content);
        when(content.getBodyPart(0)).thenReturn(part1);
        when(content.getBodyPart(1)).thenReturn(part2);
        when(emailMessage.getReceivedDate()).thenReturn(MESSAGE_DATE);
        User user = TestUtils.getUser();
        UserEmail userEmail = new UserEmail().setUser(user);
        when(userEmailService.get(anyList())).thenReturn(userEmail);

        Message message = messageMapper.toMessage(emailMessage);

        assertNotNull(message);

        Chat chat = message.getChat();
        assertNotNull(chat);
        assertEquals(user.getUserId(), chat.getChatId());
        assertNull(message.getMessageId());
        assertEquals(messageText, message.getText());
        assertEquals(MESSAGE_DATE_TIME, message.getDateTime());
        assertEquals(MessageKind.COMMON, message.getMessageKind());
        assertEquals(MessageContentType.FILE, message.getMessageContentType());
        assertNull(message.getReplyToMessage());

        List<Attachment> attachments = message.getAttachments();
        assertFalse(attachments.isEmpty());
        assertEquals(1, attachments.size());
        Attachment attachment = attachments.get(0);
        assertEquals("application", attachment.getMimeType());
        assertNull(attachment.getFileUniqueId());
        assertNull(attachment.getFileId());
        assertEquals(attachmentBytes, attachment.getFile());
        assertEquals("filename.txt", attachment.getName());
        assertEquals(attachmentBytes.length, attachment.getSize());
        assertNull(attachment.getDuration());
        assertNull(attachment.getText());

        verify(userEmailService).get(emailAddressesCaptor.capture());
        List<String> actualEmailAddresses = emailAddressesCaptor.getValue();
        assertEquals(1, actualEmailAddresses.size());
        assertEquals(emailAddress, actualEmailAddresses.get(0));
    }

}