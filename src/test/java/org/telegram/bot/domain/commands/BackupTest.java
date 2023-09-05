package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.telegram.bot.TestUtils.getUpdateFromGroup;

@ExtendWith(MockitoExtension.class)
class BackupTest {

    @Mock
    private Bot bot;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    @Test
    void parseTest() {
        Update update = getUpdateFromGroup();
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);

        Backup backup = new Backup(bot);
        backup.entityManager = entityManager;
        SendDocument sendDocument = backup.parse(update);
        verify(bot).sendUploadDocument(update);
        verify(query).executeUpdate();

        assertNotNull(sendDocument);
        assertNotNull(sendDocument.getChatId());
        assertNotNull(sendDocument.getDocument());
        assertTrue(sendDocument.getDisableNotification());
    }
}