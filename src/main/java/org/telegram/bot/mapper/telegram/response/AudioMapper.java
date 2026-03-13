package org.telegram.bot.mapper.telegram.response;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.objects.InputFile;

@RequiredArgsConstructor
@Component
public class AudioMapper implements TelegramFileApiMethodMapper {

    private final InputFileMapper inputFileMapper;

    @Override
    public FileType getMappingFileType() {
        return FileType.AUDIO;
    }

    @Override
    public PartialBotApiMethod<?> map(FileResponse fileResponse) {
        File file = fileResponse.getFiles().getFirst();
        InputFile inputFile = inputFileMapper.toInputFile(file);

        SendAudio sendAudio = new SendAudio(fileResponse.getChatId().toString(), inputFile);
        sendAudio.setDuration((int) file.getFileSettings().getDuration());
        sendAudio.setReplyToMessageId(fileResponse.getReplyToMessageId());

        return sendAudio;
    }

}
