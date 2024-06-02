package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.FileResponse;

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
        BotRequest request = TestUtils.getRequestFromGroup("logs tratatam-tratatam");

        List<BotResponse> botResponseList = logs.parse(request);

        assertTrue(botResponseList.isEmpty());
        verify(bot, never()).sendUploadDocument(anyLong());
    }

    @Test
    void parseFromGroupChatTest() {
        BotRequest request = TestUtils.getRequestFromGroup("logs");
        Long expectedChatId = request.getMessage().getUser().getUserId();

        BotResponse botResponse = logs.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseParams(botResponse);
        assertEquals(expectedChatId, fileResponse.getChatId());
        verify(bot).sendUploadDocument(request.getMessage().getChatId());
    }

    @Test
    void parseFromPrivateChatTest() {
        BotRequest request = TestUtils.getRequestFromPrivate("logs");
        Long expectedChatId = request.getMessage().getChatId();

        BotResponse botResponse = logs.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseParams(botResponse);
        assertEquals(expectedChatId, fileResponse.getChatId());
        verify(bot).sendUploadDocument(expectedChatId);
    }

}