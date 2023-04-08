package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.domain.CommandParent;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.io.File;

@Component
@RequiredArgsConstructor
@Slf4j
public class Backup implements CommandParent<SendDocument> {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional
    public SendDocument parse(Update update) {
        if (cutCommandInText(getMessageFromUpdate(update).getText()) != null) {
            return null;
        }
        return getDbBackup(update.getMessage().getFrom().getId().toString());
    }

    /**
     * Creating backup of database and sending file to chat.
     *
     * @param chatId сhat where the file will be sent.
     * @return document sending object.
     */
    @Transactional
    public SendDocument getDbBackup(String chatId) {
        log.debug("Request to send backup to {}", chatId);
        entityManager.createNativeQuery("BACKUP TO 'backup.zip'").executeUpdate();

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(new InputFile(new File("backup.zip")));
        sendDocument.setDisableNotification(true);

        return sendDocument;
    }
}
