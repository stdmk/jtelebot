package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.telegram.bot.TestUtils.getUpdate;

@ExtendWith(MockitoExtension.class)
class BackupTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    @InjectMocks
    private Backup backup;

    @Test
    void parseTest() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);

        SendDocument sendDocument = backup.parse(getUpdate());
        verify(query).executeUpdate();

        assertNotNull(sendDocument);
        assertNotNull(sendDocument.getChatId());
        assertNotNull(sendDocument.getDocument());
        assertTrue(sendDocument.getDisableNotification());
    }
}