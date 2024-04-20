package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogsTest {

    @Mock
    private Bot bot;

    @InjectMocks
    private Logs logs;

    @Test
    void parseWithArgumentsTest() {
        Update update = TestUtils.getUpdateFromGroup("logs tratatam-tratatam");

        List<SendDocument> sendDocumentList = logs.parse(update);

        assertTrue(sendDocumentList.isEmpty());
        verify(bot, never()).sendUploadDocument(anyLong());
    }

    @Test
    void parseFromGroupChatTest() {
        Update update = TestUtils.getUpdateFromGroup("logs");
        Long expectedChatId = update.getMessage().getFrom().getId();

        SendDocument sendDocument = logs.parse(update).get(0);

        TestUtils.checkDefaultSendDocumentParams(sendDocument);
        assertEquals(expectedChatId.toString(), sendDocument.getChatId());
        verify(bot).sendUploadDocument(update.getMessage().getChatId());
    }

    @Test
    void parseFromPrivateChatTest() {
        Update update = TestUtils.getUpdateFromPrivate("logs");
        Long expectedChatId = update.getMessage().getChatId();

        SendDocument sendDocument = logs.parse(update).get(0);

        TestUtils.checkDefaultSendDocumentParams(sendDocument);
        assertEquals(expectedChatId.toString(), sendDocument.getChatId());
        verify(bot).sendUploadDocument(expectedChatId);
    }

}