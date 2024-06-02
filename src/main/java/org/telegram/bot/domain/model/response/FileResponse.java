package org.telegram.bot.domain.model.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.enums.FormattingStyle;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class FileResponse implements BotResponse {

    private final List<File> files = new ArrayList<>(1);

    private Long chatId;
    private String text;
    private Integer replyToMessageId;
    private ResponseSettings responseSettings;

    public FileResponse(Message message) {
        this.chatId = message.getChatId();
        this.replyToMessageId = message.getMessageId();
    }

    public FileResponse addFile(File file) {
        this.files.add(file);
        return this;
    }

    public FileResponse addFiles(List<File> files) {
        this.files.addAll(files);
        return this;
    }

    @Tolerate
    public FileResponse setResponseSettings(FormattingStyle formattingStyle) {
        this.responseSettings = new ResponseSettings().setFormattingStyle(formattingStyle);
        return this;
    }
}
