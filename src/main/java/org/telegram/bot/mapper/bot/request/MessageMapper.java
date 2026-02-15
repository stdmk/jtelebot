package org.telegram.bot.mapper.bot.request;

import jakarta.annotation.Nullable;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.UserEmail;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.bot.domain.model.request.MessageKind;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.UserEmailService;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.reactions.MessageReactionUpdated;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.telegram.bot.utils.DateUtils.unixTimeToLocalDateTime;

@RequiredArgsConstructor
@Component
@Slf4j
public class MessageMapper {

    private final ChatMapper chatMapper;
    private final UserMapper userMapper;
    private final AttachmentMapper attachmentMapper;
    private final ReactionMapper reactionMapper;
    private final UserEmailService userEmailService;
    private final BotStats botStats;

    public Message toMessage(org.telegram.telegrambots.meta.api.objects.message.Message telegramMessage, User telegramUser, String messageText, MessageKind messageKind) {
        Pair<MessageContentType, List<Attachment>> messageContent = attachmentMapper.toAttachments(telegramMessage);

        return new Message()
                .setChat(chatMapper.toChat(telegramMessage.getChat()))
                .setUser(userMapper.toUser(telegramUser))
                .setMessageId(telegramMessage.getMessageId())
                .setText(messageText)
                .setDateTime(telegramMessage.getDate() == null ? null : unixTimeToLocalDateTime(telegramMessage.getDate()))
                .setEditDateTime(telegramMessage.getEditDate() == null ? null : unixTimeToLocalDateTime(telegramMessage.getEditDate()))
                .setMessageKind(messageKind)
                .setMessageContentType(messageContent.getKey())
                .setAttachments(messageContent.getValue())
                .setReplyToMessage(toReplyMessage(telegramMessage.getReplyToMessage()));
    }

    private Message toReplyMessage(org.telegram.telegrambots.meta.api.objects.message.Message telegramMessage) {
        String messageText;
        org.telegram.telegrambots.meta.api.objects.User telegramUser;
        MessageKind messageKind;
        if (telegramMessage == null) {
            return null;
        } else {
            telegramUser = telegramMessage.getFrom();
            messageText = telegramMessage.getText();
            messageKind = MessageKind.COMMON;
        }

        if (messageText == null) {
            messageText = telegramMessage.getCaption();
        }

        return toMessage(telegramMessage, telegramUser, messageText, messageKind);
    }

    public Message toMessage(MessageReactionUpdated messageReactionUpdated) {
        return new Message()
                .setChat(chatMapper.toChat(messageReactionUpdated.getChat()))
                .setUser(userMapper.toUser(messageReactionUpdated.getUser()))
                .setMessageId(messageReactionUpdated.getMessageId())
                .setDateTime(messageReactionUpdated.getDate() == null ? null : unixTimeToLocalDateTime(messageReactionUpdated.getDate()))
                .setMessageKind(MessageKind.COMMON)
                .setMessageContentType(MessageContentType.REACTION)
                .setReactions(reactionMapper.toReactions(messageReactionUpdated.getOldReaction(), messageReactionUpdated.getNewReaction()));
    }

    @Nullable
    public Message toMessage(jakarta.mail.Message emailMessage) {
        try {
            Address[] emailAddresses = emailMessage.getFrom();

            List<String> emails = Arrays.stream(emailAddresses)
                    .filter(InternetAddress.class::isInstance)
                    .map(InternetAddress.class::cast)
                    .map(InternetAddress::getAddress)
                    .filter(Objects::nonNull)
                    .toList();
            UserEmail userEmail = userEmailService.get(emails);
            if (userEmail == null) {
                log.info("Incoming message from an unknown address: {}", emails);
                return null;
            }
            org.telegram.bot.domain.entities.User user = userEmail.getUser();

            String text = null;
            List<Attachment> attachments = new ArrayList<>();

            Object content = emailMessage.getContent();
            if (emailMessage instanceof MimeMessage) {
                if (content instanceof Multipart multipart) {
                    for (int i = 0; i < multipart.getCount(); i++) {
                        BodyPart part = multipart.getBodyPart(i);
                        attachments.addAll(extractAttachments(part));

                        if (text == null) {
                            text = extractText(part);
                        }
                    }
                } else {
                    text = emailMessage.getContent().toString().trim();
                }
            } else {
                String errorText = "Unexpected type of email message: " + emailMessage.getClass().getName();
                log.error(errorText);
                botStats.incrementErrors(emailMessage, errorText);
                return null;
            }

            return new Message()
                    .setChat(new Chat().setChatId(user.getUserId()))
                    .setUser(user)
                    .setMessageId(null)
                    .setText(text)
                    .setDateTime(getDateTime(emailMessage))
                    .setEditDateTime(null)
                    .setMessageKind(MessageKind.COMMON)
                    .setMessageContentType(getMessageContentType(attachments))
                    .setAttachments(attachments)
                    .setReplyToMessage(null);
        } catch (MessagingException | IOException e) {
            String errorMessage = "Failed to map email-message: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(emailMessage, e, errorMessage);
            return null;
        }
    }

    private List<Attachment> extractAttachments(Part part) throws MessagingException, IOException {
        List<Attachment> attachments = new ArrayList<>();

        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                attachments.addAll(extractAttachments(mp.getBodyPart(i)));
            }
            return attachments;
        }

        String disposition = part.getDisposition();
        String fileName = part.getFileName();

        boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(disposition)
                || Part.INLINE.equalsIgnoreCase(disposition)
                || fileName != null;

        if (isAttachment) {
            try (InputStream is = part.getInputStream()) {
                byte[] bytes = is.readAllBytes();

                attachments.add(new Attachment()
                        .setName(part.getFileName())
                        .setFile(bytes)
                        .setSize((long) part.getSize())
                        .setMimeType(part.getContentType()));
            }
        }

        return attachments;
    }

    private String extractText(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            return part.getContent().toString().trim();
        }

        if (part.isMimeType("text/html")) {
            return part.getContent().toString().trim();
        }

        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String text = extractText(mp.getBodyPart(i));
                if (text != null) return text.trim();
            }
        }

        return null;
    }

    private LocalDateTime getDateTime(jakarta.mail.Message emailMessage) {
        try {
            Date receivedDate = emailMessage.getReceivedDate();
            if (receivedDate == null) {
                return null;
            }

            return LocalDateTime.ofInstant(receivedDate.toInstant(), ZoneId.systemDefault());
        } catch (MessagingException e) {
            String errorMessage = "Failed to read email date: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(emailMessage, e, errorMessage);
            return null;
        }
    }

    private MessageContentType getMessageContentType(List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return MessageContentType.TEXT;
        }

        String mimeType = attachments.getFirst().getMimeType();
        if (mimeType == null || mimeType.isBlank()) {
            return MessageContentType.TEXT;
        }

        if (mimeType.startsWith("image")) {
            return MessageContentType.PHOTO;
        } else if (mimeType.startsWith("video")) {
            return MessageContentType.VIDEO;
        } else if (mimeType.startsWith("audio")) {
            return MessageContentType.AUDIO;
        } else if (mimeType.startsWith("application")) {
            return MessageContentType.FILE;
        } else {
            return MessageContentType.UNKNOWN;
        }
    }

}
