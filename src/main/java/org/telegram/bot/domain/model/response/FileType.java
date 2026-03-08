package org.telegram.bot.domain.model.response;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum FileType {
    IMAGE("image/*"),
    VIDEO("video/*"),
    AUDIO("audio/*"),
    VOICE("audio/*"),
    FILE("application/octet-stream"),
    ;

    private final String mimeType;

    @NotNull
    public static FileType getByMimeType(String mimeType) {
        if (mimeType == null) {
            return FILE;
        } else if (mimeType.startsWith("image")) {
            return IMAGE;
        } else if (mimeType.startsWith("video")) {
            return VIDEO;
        } else if (mimeType.startsWith("audio")) {
            return VOICE;
        } else {
            return FILE;
        }
    }

}
