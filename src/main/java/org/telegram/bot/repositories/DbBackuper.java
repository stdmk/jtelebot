package org.telegram.bot.repositories;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.File;

@Component
@Slf4j
public class DbBackuper {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Creating backup of database and sending file to chat.
     *
     * @return document sending object.
     */
    @Transactional
    public File getDbBackup() {
        entityManager.createNativeQuery("BACKUP TO 'backup.zip'").executeUpdate();
        return new File("backup.zip");
    }

}
