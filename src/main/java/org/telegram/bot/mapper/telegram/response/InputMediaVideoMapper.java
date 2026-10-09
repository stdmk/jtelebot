package org.telegram.bot.mapper.telegram.response;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileSettings;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;

import java.util.Set;

@Component
public class InputMediaVideoMapper implements InputMediaMapper {

    @Override
    public Set<FileType> getSupportedFileTypes() {
        return Set.of(FileType.VIDEO);
    }

    @Override
    public InputMedia map(File file) {
        InputMediaVideo video = InputMediaMapperSupport.create(
                file, InputMediaVideo::new, InputMediaVideo::new, InputMediaVideo::new);
        FileSettings settings = file.getFileSettings();
        video.setDuration((int) settings.getDuration());
        video.setWidth(settings.getWidth());
        video.setHeight(settings.getHeight());
        video.setSupportsStreaming(true);
        video.setHasSpoiler(settings.isSpoiler());
        return video;
    }
}
