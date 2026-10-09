package org.telegram.bot.mapper.telegram.response;

import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;

import java.util.Set;

public interface InputMediaMapper {

    Set<FileType> getSupportedFileTypes();

    InputMedia map(File file);
}
