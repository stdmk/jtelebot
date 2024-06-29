package org.telegram.bot.commands;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.coordinates.Coordinates;
import org.telegram.bot.utils.coordinates.CoordinatesUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class Metadata implements Command {

    private static final Integer INCOME_FILE_SIZE_LIMIT_BYTES = 20971520;
    private static final String GPS_DIRECTORY = "GPS";
    private static final String GPS_LATITUDE_TAG = "GPS Latitude";
    private static final String GPS_LONGITUDE_TAG = "GPS Longitude";

    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final Bot bot;
    private final BotStats botStats;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        Long chatId = message.getChatId();

        String commandArgument = commandWaitingService.getText(message);

        if (commandArgument != null) {
            return Collections.emptyList();
        }

        String responseText;
        Integer repliedMessageId;
        bot.sendTyping(chatId);

        Message messageWithFile = null;
        if (!message.hasAttachment()) {
            Message replyToMessage = message.getReplyToMessage();
            if (replyToMessage != null && replyToMessage.hasAttachment()) {
                messageWithFile = replyToMessage;
            }
        } else {
            messageWithFile = message;
        }

        if (messageWithFile == null) {
            repliedMessageId = message.getMessageId();
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.metadata.commandwaitingstart}";
        } else {
            repliedMessageId = messageWithFile.getMessageId();

            try (InputStream file = getFileFromMessage(messageWithFile.getAttachments().get(0))) {
                try {
                    responseText = getMetadata(file);
                } catch (ImageProcessingException e) {
                    log.error("Failed to get metadata from file", e);
                    botStats.incrementErrors(request, e, "Failed to get metadata from file");
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }
            } catch (IOException e) {
                log.error("Failed to get metadata from file", e);
                botStats.incrementErrors(request, e, "Failed to get metadata from file");
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }

        return returnResponse(new TextResponse()
                .setChatId(message.getChatId())
                .setReplyToMessageId(repliedMessageId)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML));
    }

    private InputStream getFileFromMessage(Attachment attachment) {
        String fileId = attachment.getFileId();
        long fileSize = attachment.getSize();

        checkFileSizeLimit(fileSize);

        return bot.getInputStreamFromTelegramFile(fileId);
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
                return formatCoordinateForCommand(coordinates.latitude()) + "_"
                        + formatCoordinateForCommand(coordinates.longitude());
            }
        }

        return null;
    }

    private String formatCoordinateForCommand(Double coordinate) {
        return String.format("%.4f", coordinate).replace("\\.", "_").replace(",", "_");
    }

}
