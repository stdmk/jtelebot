package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.repositories.DbBackuper;
import org.telegram.bot.services.executors.SendDocumentExecutor;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackupTimer extends TimerParent {

    private final SendDocumentExecutor sendDocumentExecutor;
    private final PropertiesConfig propertiesConfig;
    private final DbBackuper dbBackuper;

    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void execute() {
        InputFile dbBackup = dbBackuper.getDbBackup();

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(propertiesConfig.getAdminId().toString());
        sendDocument.setDocument(dbBackup);
        sendDocument.setDisableNotification(true);

        sendDocumentExecutor.executeMethod(sendDocument);
    }
}
