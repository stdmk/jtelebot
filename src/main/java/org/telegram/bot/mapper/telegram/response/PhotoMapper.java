package org.telegram.bot.mapper.telegram.response;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

@RequiredArgsConstructor
@Component
public class PhotoMapper implements TelegramFileApiMethodMapper {

    private final InputFileMapper inputFileMapper;
    private final ParseModeMapper parseModeMapper;

    @Override
    public FileType getMappingFileType() {
        return FileType.IMAGE;
    }

    @Override
    public PartialBotApiMethod<?> map(FileResponse fileResponse) {
        org.telegram.bot.domain.model.response.File file = fileResponse.getFiles().get(0);

        SendPhoto sendPhoto = new SendPhoto(fileResponse.getChatId().toString(), inputFileMapper.toInputFile(file));
        sendPhoto.setReplyToMessageId(fileResponse.getReplyToMessageId());
        sendPhoto.setCaption(fileResponse.getText());

        ResponseSettings responseSettings = fileResponse.getResponseSettings();
        if (responseSettings != null) {
            if (Boolean.FALSE.equals(responseSettings.getNotification())) {
                sendPhoto.disableNotification();
            }

            sendPhoto.setParseMode(parseModeMapper.toParseMode(responseSettings.getFormattingStyle()));
        }
        if (file.getFileSettings().isSpoiler()) {
            sendPhoto.setHasSpoiler(true);
        }

        return sendPhoto;
    }

}
