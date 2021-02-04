package org.telegram.bot;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.exception.BotException;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;

import static org.telegram.bot.utils.NetworkUtils.getFileFromUrl;

@AllArgsConstructor
public class Parser extends Thread {

    private final Logger log = LoggerFactory.getLogger(Parser.class);

    private final Bot bot;
    private final CommandParent<?> command;
    private final Update update;

    @Override
    public void run() {
        if (command == null) {
            return;
        }
        log.debug("Find a command {}", command.toString());
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
            log.error("Error: cannot send chat action: {}", e.getMessage());
        }

        try {
            PartialBotApiMethod<?> method = command.parse(update);
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
            } else if (method instanceof EditMessageText) {
                EditMessageText editMessageText = (EditMessageText) method;
                log.info("To " + message.getChatId() + ": edited message " + editMessageText.getText());
                bot.execute(editMessageText);
            } else if (method instanceof SendDocument) {
                SendDocument sendDocument = (SendDocument) method;
                log.info("To " + message.getChatId() + ": sending document " + sendDocument.getCaption());
                bot.execute(sendDocument);
            }
        } catch (TelegramApiException e) {
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
            e.printStackTrace();
        }
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
            try {
                sendPhoto.setPhoto(new InputFile(getFileFromUrl(inputMedia.getMedia(), 5000000), "image"));
                bot.execute(sendPhoto);
            } catch (Exception exception) {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(sendMediaGroup.getChatId());
                sendMessage.setReplyToMessageId(sendMediaGroup.getReplyToMessageId());
                sendMessage.setText("Не удалось загрузить картинку по адресу: " + inputMedia.getMedia() + "\n" + buf.toString());

                try {
                    bot.execute(sendMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
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
                        try {
                            sendPhoto.setPhoto(new InputFile(getFileFromUrl(inputMedia.getMedia(), 5000000), "image"));
                            bot.execute(sendPhoto);
                        } catch (Exception exception) {
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
                    }
                });
    }

    private void tryToDeliverTheMessage(SendPhoto sendPhoto) throws TelegramApiException {
        String imageUrl = sendPhoto.getPhoto().getAttachName();
        try {
            InputStream image = getFileFromUrl(imageUrl, 5000000);
            sendPhoto.setPhoto(new InputFile(image, "google"));

            bot.execute(sendPhoto);
        } catch (Exception e) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setReplyToMessageId(sendPhoto.getReplyToMessageId());
            sendMessage.setChatId(sendPhoto.getChatId());
            sendMessage.setText(sendPhoto.getCaption() + "\nНе удалось загрузить картинку по адресу: " + imageUrl);
            sendMessage.enableHtml(true);
            sendMessage.disableWebPagePreview();

            bot.execute(sendMessage);
        }
    }
}
