package org.telegram.bot.mapper.telegram.response;

import org.telegram.bot.domain.model.response.File;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.BiFunction;
import java.util.function.Function;

final class InputMediaMapperSupport {

    private InputMediaMapperSupport() {
    }

    static <T extends InputMedia> T create(File file, Function<String, T> fromReference, BiFunction<java.io.File, String, T> fromDiskFile, BiFunction<InputStream, String, T> fromBytes) {
        T media;
        if (file.getFileId() != null) {
            media = fromReference.apply(file.getFileId());
        } else if (file.getUrl() != null) {
            media = fromReference.apply(file.getUrl());
        } else if (file.getBytes() != null) {
            media = fromBytes.apply(new ByteArrayInputStream(file.getBytes()), file.getName());
        } else if (file.getDiskFile() != null) {
            media = fromDiskFile.apply(file.getDiskFile(), file.getName());
        } else {
            throw new IllegalArgumentException("Unknown type of File: " + file);
        }

        media.setCaption(file.getName());
        return media;
    }
}
