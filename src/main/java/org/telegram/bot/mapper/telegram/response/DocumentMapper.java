package org.telegram.bot.mapper.telegram.response;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;

@RequiredArgsConstructor
@Component
public class DocumentMapper implements TelegramFileApiMethodMapper {

    private final InputFileMapper inputFileMapper;
    private final ParseModeMapper parseModeMapper;

    @Override
    public FileType getMappingFileType() {
        return FileType.FILE;
    }

    @Override
    public PartialBotApiMethod<?> map(FileResponse fileResponse) {
        InputFile inputFile = inputFileMapper.toInputFile(fileResponse.getFiles().get(0));

        SendDocument sendDocument = new SendDocument(fileResponse.getChatId().toString(), inputFile);
        sendDocument.setReplyToMessageId(fileResponse.getReplyToMessageId());
        sendDocument.setCaption(fileResponse.getText());

        ResponseSettings responseSettings = fileResponse.getResponseSettings();
        if (responseSettings != null) {
            if (Boolean.FALSE.equals(responseSettings.getNotification())) {
                sendDocument.disableNotification();
            }

            sendDocument.setParseMode(parseModeMapper.toParseMode(responseSettings.getFormattingStyle()));
        }

        return sendDocument;
    }

}
