package org.telegram.bot.mapper.telegram.response;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;

import java.util.List;

@Component
public class MediaGroupMapper {

    public PartialBotApiMethod<?> map(FileResponse fileResponse) {
        List<InputMedia> images = fileResponse.getFiles()
                .stream()
                .map(this::toInputMedia)
                .toList();

        SendMediaGroup sendMediaGroup = new SendMediaGroup(fileResponse.getChatId().toString(), images);
        sendMediaGroup.setReplyToMessageId(fileResponse.getReplyToMessageId());

        return sendMediaGroup;
    }

    private InputMedia toInputMedia(File file) {
        InputMediaPhoto inputMediaPhoto = new InputMediaPhoto(file.getUrl());
        inputMediaPhoto.setCaption(file.getName());
        inputMediaPhoto.setHasSpoiler(file.getFileSettings().isSpoiler());
        return inputMediaPhoto;
    }

}
