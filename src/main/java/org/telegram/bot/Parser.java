package org.telegram.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.exception.BotException;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

@RequiredArgsConstructor
@Slf4j
public class Parser extends Thread {

    private final Bot bot;
    private final CommandParent<?> command;
    private final Update update;
    private final BotStats botStats;

    @Override
    public void run() {
        if (command == null) {
            return;
        }
        log.debug("Find a command {}", command);
        Message message = update.getMessage();
        if (message == null) {
            message = update.getEditedMessage();
            if (message == null) {
                message = update.getCallbackQuery().getMessage();
            }
        }

        SendChatAction sendChatAction = new SendChatAction();
        sendChatAction.setChatId(message.getChatId().toString());
        sendChatAction.setAction(ActionType.TYPING);
        try {
            bot.execute(sendChatAction);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(update, sendChatAction, e, "ошибка при отправке Action печатает...");
            log.error("Error: cannot send chat action: {}", e.getMessage());
        }

        PartialBotApiMethod<?> method;
        try {
            method = command.parse(update);
        } catch (Exception e) {
            botStats.incrementErrors(update, e, "неожиданная ошибка при обработке команды ботом");
            log.error("Unexpected error: ", e);
            return;
        }

        try {
            if (method == null) {
                return;
            }
            if (method instanceof SendMessage) {
                SendMessage sendMessage = (SendMessage) method;
                log.info("To " + message.getChatId() + ": " + sendMessage.getText());
                bot.execute(sendMessage);
            } else if (method instanceof SendPhoto) {
                SendPhoto sendPhoto = (SendPhoto) method;
                log.info("To " + message.getChatId() + ": sending photo " + sendPhoto.getCaption());
                try {
                    bot.execute(sendPhoto);
                } catch (TelegramApiException e) {
                    tryToDeliverTheMessage(sendPhoto);
                }
            } else if (method instanceof SendMediaGroup) {
                SendMediaGroup sendMediaGroup = (SendMediaGroup) method;
                log.info("To " + message.getChatId() + ": sending photos " + sendMediaGroup);
                try {
                    bot.execute(sendMediaGroup);
                } catch (TelegramApiException e) {
                    tryToSendOnePhoto(sendMediaGroup);
                }
            } else if (method instanceof SendVideo) {
                SendVideo sendVideo = (SendVideo) method;
                log.info("To " + message.getChatId() + ": " + sendVideo.getCaption());
                bot.execute(sendVideo);
            } else if (method instanceof EditMessageText) {
                EditMessageText editMessageText = (EditMessageText) method;
                log.info("To " + message.getChatId() + ": edited message " + editMessageText.getText());
                bot.execute(editMessageText);
            } else if (method instanceof SendDocument) {
                SendDocument sendDocument = (SendDocument) method;
                log.info("To " + message.getChatId() + ": sending document " + sendDocument.getCaption());
                bot.execute(sendDocument);
            } else if (method instanceof DeleteMessage) {
                DeleteMessage deleteMessage = (DeleteMessage) method;
                log.info("Deleting message {}", deleteMessage.getMessageId());
                bot.execute(deleteMessage);
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

                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                log.error("Error: cannot send response: {}", e.getMessage());
            }
        } catch (Exception e) {
            botStats.incrementErrors(update, method, e, "неожиданная верхнеуровневая ошибка");
            log.error("Unexpected error: ", e);
        }

        botStats.incrementCommandsProcessed();
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
            bot.execute(sendPhoto);
        } catch (TelegramApiException telegramApiException) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(sendMediaGroup.getChatId());
            sendMessage.setReplyToMessageId(sendMediaGroup.getReplyToMessageId());
            sendMessage.setText("Не удалось загрузить картинку по адресу: " + inputMedia.getMedia() + "\n" + buf);

            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void splitSendMediaGroup(SendMediaGroup sendMediaGroup) {
        sendMediaGroup.getMedias()
                .forEach(inputMedia -> {
                    InputFile inputFile = new InputFile();
                    inputFile.setMedia(inputMedia.getMedia());

                    SendPhoto sendPhoto = new SendPhoto();
                    sendPhoto.setPhoto(inputFile);
                    sendPhoto.setReplyToMessageId(sendMediaGroup.getReplyToMessageId());
                    sendPhoto.setChatId(sendMediaGroup.getChatId());

                    try {
                        bot.execute(sendPhoto);
                    } catch (TelegramApiException telegramApiException) {
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(sendMediaGroup.getChatId());
                        sendMessage.setReplyToMessageId(sendMediaGroup.getReplyToMessageId());
                        sendMessage.setText("Не удалось загрузить картинку по адресу: " + inputMedia.getMedia());

                        try {
                            bot.execute(sendMessage);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void tryToDeliverTheMessage(SendPhoto sendPhoto) throws TelegramApiException {
        String imageUrl = sendPhoto.getPhoto().getAttachName();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyToMessageId(sendPhoto.getReplyToMessageId());
        sendMessage.setChatId(sendPhoto.getChatId());
        sendMessage.setText("Не удалось отправить картинку с адреса: " + imageUrl + "\n" + sendPhoto.getCaption());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();

        bot.execute(sendMessage);
    }
}
