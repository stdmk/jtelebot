package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.repositories.DbBackuper;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@Slf4j
public class Backup implements Command<SendDocument> {

    private final Bot bot;
    private final DbBackuper dbBackuper;

    @Override
    public SendDocument parse(Update update) {
        String chatId = update.getMessage().getFrom().getId().toString();
        log.debug("Request to send backup to {}", chatId);

        bot.sendUploadDocument(update);

        if (cutCommandInText(getMessageFromUpdate(update).getText()) != null) {
            return null;
        }

        InputFile dbBackup = dbBackuper.getDbBackup();

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(dbBackup);
        sendDocument.setDisableNotification(true);

        return sendDocument;
    }


}
