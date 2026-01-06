package org.telegram.bot.mapper.email.response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.bot.services.BotStats;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Component
@Slf4j
public class FileMapper {

    @Lazy
    private final Bot bot;
    private final BotStats botStats;

    public List<File> toFiles(List<Attachment> attachments) {
        if (attachments != null) {
            return attachments
                    .stream()
                    .map(this::toFile)
                    .filter(Objects::nonNull)
                    .toList();
        }

        return null;
    }

    public File toFile(Attachment attachment) {
        byte[] file = attachment.getFile();
        if (file == null) {
            try {
                file = bot.getBytesTelegramFile(attachment.getFileId());
            } catch (TelegramApiException | IOException e) {
                String errorMessage = "Failed to read file from telegram";
                log.error(errorMessage);
                botStats.incrementErrors(attachment, e, errorMessage);
                return null;
            }
        }

        return new File(FileType.getByMimeType(attachment.getMimeType()), file, attachment.getName(), attachment.getText());
    }

}
