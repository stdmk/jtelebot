package org.telegram.bot.mapper.telegram.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageKind;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.reactions.MessageReactionUpdated;

@Component
@RequiredArgsConstructor
public class RequestMapper {

    private final MessageMapper messageMapper;

    public BotRequest toBotRequest(Update update) {
        Message message;

        MessageReactionUpdated messageReaction = update.getMessageReaction();
        if (messageReaction != null) {
            message = messageMapper.toMessage(messageReaction);
        } else {
            message = getMessage(update);
        }

        return new BotRequest().setMessage(message);
    }

    private Message getMessage(Update update) {
        String messageText;
        org.telegram.telegrambots.meta.api.objects.User telegramUser;
        MessageKind messageKind;
        org.telegram.telegrambots.meta.api.objects.message.Message telegramMessage = update.getMessage();
        if (telegramMessage == null) {
            telegramMessage = update.getEditedMessage();
            if (telegramMessage == null) {
                CallbackQuery callbackQuery = update.getCallbackQuery();
                if (callbackQuery == null) {
                    return null;
                } else {
                    telegramMessage = (org.telegram.telegrambots.meta.api.objects.message.Message) callbackQuery.getMessage();
                    telegramUser = callbackQuery.getFrom();
                    messageText = callbackQuery.getData();
                    messageKind = MessageKind.CALLBACK;
                }
            } else {
                telegramUser = telegramMessage.getFrom();
                messageText = telegramMessage.getText();
                messageKind = MessageKind.EDIT;
            }
        } else {
            telegramUser = telegramMessage.getFrom();
            messageText = telegramMessage.getText();
            messageKind = MessageKind.COMMON;
        }

        if (messageText == null) {
            messageText = telegramMessage.getCaption();
        }

        return messageMapper.toMessage(telegramMessage, telegramUser, messageText, messageKind);
    }

}
