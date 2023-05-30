package org.telegram.bot.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.domain.entities.ChatGPTMessage;
import org.telegram.bot.repositories.ChatGPTMessageRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ChatGPTMessageServiceImplTest {
    @Mock
    private ChatGPTMessageRepository chatGPTMessageRepository;

    @Captor
    private ArgumentCaptor<List<ChatGPTMessage>> captor;

    @InjectMocks
    private ChatGPTMessageServiceImpl chatGPTMessageService;

    @Test
    void updateFromUserTest() {
        List<ChatGPTMessage> chatGPTMessages = LongStream.range(1, 203).mapToObj(n -> new ChatGPTMessage().setId(n)).collect(Collectors.toList());

        chatGPTMessageService.update(chatGPTMessages);

        Mockito.verify(chatGPTMessageRepository).deleteAll(captor.capture());
        List<ChatGPTMessage> deletingChatGPTMessages = captor.getValue();
        assertEquals(2, deletingChatGPTMessages.size());
        assertFalse(deletingChatGPTMessages.stream().anyMatch(chatGPTMessage -> chatGPTMessage.getId() > 2));
    }
}