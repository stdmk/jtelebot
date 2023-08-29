package org.telegram.bot.domain.commands;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.bot.utils.coordinates.Coordinates;
import org.telegram.bot.utils.coordinates.CoordinatesUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor
@Component
@Slf4j
public class Metadata implements CommandParent<SendMessage> {

    private static final Integer INCOME_FILE_SIZE_LIMIT_BYTES = 20971520;
    private static final String GPS_DIRECTORY = "GPS";
    private static final String GPS_LATITUDE_TAG = "GPS Latitude";
    private static final String GPS_LONGITUDE_TAG = "GPS Longitude";

    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final Bot bot;
    private final NetworkUtils networkUtils;
    private final BotStats botStats;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);

        Long chatId = message.getChatId();
        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        String responseText;
        Integer repliedMessageId;
        if (textMessage == null) {
            bot.sendTyping(chatId);

            Message messageWithFile = null;
            if (!hasAnyFile(message)) {
                Message replyToMessage = message.getReplyToMessage();
                if (replyToMessage != null && hasAnyFile(replyToMessage)) {
                    messageWithFile = replyToMessage;
                }
            } else {
                messageWithFile = message;
            }

            if (messageWithFile == null) {
                repliedMessageId = message.getMessageId();
                log.debug("Empty request. Turning on command waiting");
                commandWaitingService.add(message, this.getClass());
                responseText = "теперь пришли мне файл";
            } else {
                repliedMessageId = messageWithFile.getMessageId();

                InputStream file;
                try {
                    file = getFileFromMessage(messageWithFile);
                } catch (TelegramApiException e) {
                    log.error("Failed to get file from telegram", e);
                    botStats.incrementErrors(update, e, "Failed to get file from telegram");
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                }

                try {
                    responseText = getMetadata(file);
                } catch (IOException | ImageProcessingException e) {
                    log.error("Failed to get metadata from file", e);
                    botStats.incrementErrors(update, e, "Failed to get metadata from file");
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                } finally {
                    try {
                        file.close();
                    } catch (IOException e) {
                        log.error("Failed to close inputstream of file", e);
                        botStats.incrementErrors(update, e, "Failed to close inputstream of file");
                    }
                }
            }
        } else {
            return null;
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyToMessageId(repliedMessageId);
        sendMessage.setChatId(chatId);
        sendMessage.setText(responseText);
        sendMessage.enableHtml(true);

        return sendMessage;
    }

    private boolean hasAnyFile(Message message) {
        return message.hasDocument() || message.hasVideo() || message.hasAudio() || message.hasPhoto();
    }

    private InputStream getFileFromMessage(Message message) throws TelegramApiException {
        String fileId;
        long fileSize;

        if (message.hasDocument()) {
            Document document = message.getDocument();
            fileId = document.getFileId();
            fileSize = document.getFileSize();
        } else if (message.hasVideo()) {
            Video video = message.getVideo();
            fileId = video.getFileId();
            fileSize = video.getFileSize();
        } else if (message.hasAudio()) {
            Audio audio = message.getAudio();
            fileId = audio.getFileId();
            fileSize = audio.getFileSize();
        } else if (message.hasPhoto()) {
            PhotoSize photoSize = message.getPhoto().get(message.getPhoto().size() - 1);
            fileId = photoSize.getFileId();
            fileSize = photoSize.getFileSize();
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        checkFileSizeLimit(fileSize);

        return networkUtils.getFileFromTelegram(bot, fileId);
    }

    private void checkFileSizeLimit(Long fileSize) {
        if (fileSize > INCOME_FILE_SIZE_LIMIT_BYTES) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private String getMetadata(InputStream inputStream) throws ImageProcessingException, IOException {
        com.drew.metadata.Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

        StringBuilder buf = new StringBuilder();

        metadata.getDirectories().forEach(directory -> {
            buf.append("<b><u>").append(directory.getName()).append("</u></b>\n");
            directory.getTags().forEach(tag ->
                    buf.append("<b>").append(tag.getTagName()).append("</b>: ").append(tag.getDescription()).append(";\n"));
            buf.append("\n");
        });

        String location = getLocation(metadata);
        if (location != null) {
            buf.append("/location").append("_").append(location);
        }

        return buf.toString();
    }

    private String getLocation(com.drew.metadata.Metadata metadata) {
        String latitudeData = null;
        String longitudeData = null;
        for (Directory directory : metadata.getDirectories()) {
            if (GPS_DIRECTORY.equalsIgnoreCase(directory.getName())) {
                for (Tag tag : directory.getTags()) {
                    if (latitudeData != null && longitudeData != null) {
                        break;
                    }
                    if (GPS_LATITUDE_TAG.equalsIgnoreCase(tag.getTagName())) {
                        latitudeData = tag.getDescription();
                    } else if (GPS_LONGITUDE_TAG.equalsIgnoreCase(tag.getTagName())) {
                        longitudeData = tag.getDescription();
                    }
                }
                break;
            }
        }

        if (latitudeData != null && longitudeData != null) {
            Coordinates coordinates = CoordinatesUtils.parseCoordinates(latitudeData + " " + longitudeData);
            if (coordinates != null) {
                return formatCoordinateForCommand(coordinates.getLatitude()) + "_"
                        + formatCoordinateForCommand(coordinates.getLongitude());
            }
        }

        return null;
    }

    private String formatCoordinateForCommand(Double coordinate) {
        return String.format("%.4f", coordinate).replaceAll("\\.", "_").replaceAll(",", "_");
    }

}
