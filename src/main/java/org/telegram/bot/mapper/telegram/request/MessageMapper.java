package org.telegram.bot.mapper.telegram.request;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.bot.domain.model.request.MessageKind;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.reactions.MessageReactionUpdated;

import java.util.List;

import static org.telegram.bot.utils.DateUtils.unixTimeToLocalDateTime;

@RequiredArgsConstructor
@Component
public class MessageMapper {

    private final ChatMapper chatMapper;
    private final UserMapper userMapper;
    private final AttachmentMapper attachmentMapper;
    private final ReactionMapper reactionMapper;

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

}
