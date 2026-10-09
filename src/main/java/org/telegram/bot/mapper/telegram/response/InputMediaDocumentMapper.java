package org.telegram.bot.mapper.telegram.response;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;

import java.util.Set;

@Component
public class InputMediaDocumentMapper implements InputMediaMapper {

    @Override
    public Set<FileType> getSupportedFileTypes() {
        return Set.of(FileType.FILE);
    }

    @Override
    public InputMedia map(File file) {
        return InputMediaMapperSupport.create(
                file, InputMediaDocument::new, InputMediaDocument::new, InputMediaDocument::new);
    }
}
