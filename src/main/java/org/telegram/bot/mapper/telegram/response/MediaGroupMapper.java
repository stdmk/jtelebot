package org.telegram.bot.mapper.telegram.response;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;

import java.util.List;

@RequiredArgsConstructor
@Component
public class MediaGroupMapper {

    private final List<InputMediaMapper> inputMediaMappers;

    public PartialBotApiMethod<?> map(FileResponse fileResponse) {

        List<InputMedia> medias = fileResponse.getFiles()
                .stream()
                .map(this::toInputMedia)
                .toList();

        SendMediaGroup sendMediaGroup = new SendMediaGroup(fileResponse.getChatId().toString(), medias);
        sendMediaGroup.setReplyToMessageId(fileResponse.getReplyToMessageId());

        return sendMediaGroup;
    }

    private InputMedia toInputMedia(File file) {
        InputMediaMapper mapper = inputMediaMappers.stream()
                .filter(candidate -> candidate.getSupportedFileTypes().contains(file.getFileType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No InputMedia mapper for FileType: " + file.getFileType()));
        return mapper.map(file);
    }

}
