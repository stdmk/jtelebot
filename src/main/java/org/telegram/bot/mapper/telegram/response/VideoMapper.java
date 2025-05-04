package org.telegram.bot.mapper.telegram.response;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;

@RequiredArgsConstructor
@Component
public class VideoMapper implements TelegramFileApiMethodMapper {

    private final InputFileMapper inputFileMapper;

    @Override
    public FileType getMappingFileType() {
        return FileType.VIDEO;
    }

    @Override
    public PartialBotApiMethod<?> map(FileResponse fileResponse) {
        org.telegram.bot.domain.model.response.File file = fileResponse.getFiles().get(0);
        InputFile inputFile = inputFileMapper.toInputFile(file);

        SendVideo sendVideo = new SendVideo(fileResponse.getChatId().toString(), inputFile);
        sendVideo.setReplyToMessageId(fileResponse.getReplyToMessageId());

        if (file.getFileSettings().isSpoiler()) {
            sendVideo.setHasSpoiler(true);
        }

        return sendVideo;
    }

}
