package org.telegram.bot.mapper.telegram.response;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.bot.services.BotStats;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;

import java.io.ByteArrayInputStream;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class InputMediaPhotoMapper implements InputMediaMapper {

    private final BotStats botStats;

    @Override
    public Set<FileType> getSupportedFileTypes() {
        return Set.of(FileType.IMAGE);
    }

    @Override
    public InputMedia map(File file) {
        return toInputMediaPhoto(file);
    }

    public InputMediaPhoto toInputMediaPhoto(File file) {
        InputMediaPhoto inputMediaPhoto;
        
        if (file.getFileId() != null) {
            inputMediaPhoto =  new InputMediaPhoto(file.getFileId());
        } else if (file.getUrl() != null) {
            inputMediaPhoto =  new InputMediaPhoto(file.getUrl());
        } else if (file.getBytes() != null) {
            inputMediaPhoto =  new InputMediaPhoto(new ByteArrayInputStream(file.getBytes()), file.getName());
        } else if (file.getDiskFile() != null) {
            inputMediaPhoto =  new InputMediaPhoto(file.getDiskFile(), file.getName());
        } else {
            botStats.incrementErrors(file, "unable to map file");
            throw new IllegalArgumentException("Unknown type of File: " + file);
        }

        inputMediaPhoto.setCaption(file.getName());
        inputMediaPhoto.setHasSpoiler(file.getFileSettings().isSpoiler());

        return inputMediaPhoto;
    }

}
