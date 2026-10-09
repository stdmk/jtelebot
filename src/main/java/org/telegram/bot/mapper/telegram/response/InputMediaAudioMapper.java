package org.telegram.bot.mapper.telegram.response;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaAudio;

import java.util.Set;

@Component
public class InputMediaAudioMapper implements InputMediaMapper {

    @Override
    public Set<FileType> getSupportedFileTypes() {
        return Set.of(FileType.AUDIO, FileType.VOICE);
    }

    @Override
    public InputMedia map(File file) {
        InputMediaAudio audio = InputMediaMapperSupport.create(
                file, InputMediaAudio::new, InputMediaAudio::new, InputMediaAudio::new);
        audio.setDuration((int) file.getFileSettings().getDuration());
        return audio;
    }
}
