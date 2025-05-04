package org.telegram.bot.mapper.telegram.response;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.InputFile;

@RequiredArgsConstructor
@Component
public class VoiceMapper implements TelegramFileApiMethodMapper {

    private final InputFileMapper inputFileMapper;

    @Override
    public FileType getMappingFileType() {
        return FileType.VOICE;
    }

    @Override
    public PartialBotApiMethod<?> map(FileResponse fileResponse) {
        InputFile inputFile = inputFileMapper.toInputFile(fileResponse.getFiles().get(0));

        SendVoice sendVoice = new SendVoice(fileResponse.getChatId().toString(), inputFile);
        sendVoice.setReplyToMessageId(fileResponse.getReplyToMessageId());

        return sendVoice;
    }

}
