package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.io.File;

@Component
@AllArgsConstructor
public class Backup implements CommandParent<SendDocument> {

    @PersistenceContext
    EntityManager entityManager;

    private final PropertiesConfig propertiesConfig;

    @Override
    @Transactional
    public SendDocument parse(Update update) {
        return getDbBackup(update.getMessage().getFrom().getId().toString());
    }

    @Transactional
    public SendDocument getDbBackup(String chatId) {
        entityManager.createNativeQuery("BACKUP TO 'backup.zip'").executeUpdate();

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(new InputFile(new File("backup.zip")));
        sendDocument.setDisableNotification(true);

        return sendDocument;
    }
}
