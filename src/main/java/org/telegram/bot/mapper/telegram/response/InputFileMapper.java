package org.telegram.bot.mapper.telegram.response;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.services.BotStats;
import org.telegram.telegrambots.meta.api.objects.InputFile;

@RequiredArgsConstructor
@Component
public class InputFileMapper {

    private final BotStats botStats;

    public InputFile toInputFile(org.telegram.bot.domain.model.response.File file) {
        if (file.getFileId() != null) {
            return new InputFile(file.getFileId());
        } else if (file.getUrl() != null) {
            return new InputFile(file.getUrl());
        } else if (file.getInputStream() != null) {
            return new InputFile(file.getInputStream(), file.getName());
        } else if (file.getDiskFile() != null) {
            return new InputFile(file.getDiskFile());
        } else {
            botStats.incrementErrors(file, "unable to map file");
            throw new IllegalArgumentException("Unknown type of File: " + file);
        }
    }

}
