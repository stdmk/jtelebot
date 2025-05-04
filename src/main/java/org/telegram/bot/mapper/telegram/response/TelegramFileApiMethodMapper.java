package org.telegram.bot.mapper.telegram.response;

import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;

public interface TelegramFileApiMethodMapper {
    FileType getMappingFileType();
    PartialBotApiMethod<?> map(FileResponse fileResponse);
}
