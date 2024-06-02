package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.repositories.DbBackuper;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Backup implements Command {

    private final Bot bot;
    private final DbBackuper dbBackuper;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Long chatId = message.getChatId();
        log.debug("Request to send backup to {}", chatId);

        bot.sendUploadDocument(chatId);

        if (message.hasCommandArgument()) {
            return returnResponse();
        }

        return returnResponse(new FileResponse(message)
                .addFile(new File(FileType.FILE, dbBackuper.getDbBackup()))
                .setResponseSettings(new ResponseSettings()
                        .setNotification(false)));
    }

}
