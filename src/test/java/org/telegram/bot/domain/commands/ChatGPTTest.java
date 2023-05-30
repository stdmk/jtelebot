package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatGPTMessage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.ChatGPTRole;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatGPTMessageService;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.telegram.bot.TestUtils.getUpdate;

@ExtendWith(MockitoExtension.class)
class ChatGPTTest {
    @Mock
    PropertiesConfig propertiesConfig;
    @Mock
    SpeechService speechService;
    @Mock
    CommandWaitingService commandWaitingService;
    @Mock
    ChatGPTMessageService chatGPTMessageService;
    @Mock
    ObjectMapper objectMapper;
    @Mock
    RestTemplate defaultRestTemplate;
    @Mock
    BotStats botStats;

    @Captor
    ArgumentCaptor<List<ChatGPTMessage>> captor;

    @InjectMocks
    private ChatGPT chatGPT;

    @Test
    void unavailableTokenTest() {
        when(propertiesConfig.getChatGPTToken()).thenReturn(null);

        assertThrows(BotException.class, () -> chatGPT.parse(getUpdate()));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN);
    }

    @Test
    void emptyParamTest() {
        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);

        chatGPT.parse(getUpdate());

        verify(commandWaitingService).add(any(Message.class), any(Class.class));
    }

    @Test
    void requestSerialisationError() throws JsonProcessingException {
        Update update = getUpdate("chatgpt say hello");

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenThrow(mock(JsonProcessingException.class));

        assertThrows(BotException.class, () -> chatGPT.parse(update));
        verify(botStats).incrementErrors(any(Object.class), any(JsonProcessingException.class), anyString());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void requestWithRestClientExceptionError() throws JsonProcessingException {
        Update update = getUpdate("chatgpt say hello");

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class))).thenThrow(new RestClientException("test"));

        assertThrows(BotException.class, () -> chatGPT.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void requestWithEmptyResponseTest() throws JsonProcessingException {
        Update update = getUpdate("chatgpt say hello");

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertThrows(BotException.class, () -> chatGPT.parse(update));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void messageFromChatWithEmptyHistoryTest() throws JsonProcessingException {
        final String expectedRequestText = "say hello";
        final String expectedResponseText = "hello";
        Update update = getUpdate("chatgpt " + expectedRequestText);

        ChatGPT.Message message = new ChatGPT.Message();
        message.setContent(expectedResponseText);
        ChatGPT.Choice choice = new ChatGPT.Choice();
        choice.setMessage(message);
        ChatGPT.Response response = new ChatGPT.Response();
        response.setChoices(List.of(choice));

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        SendMessage sendMessage = chatGPT.parse(update);

        verify(chatGPTMessageService).update(captor.capture());
        List<ChatGPTMessage> chatGPTMessages = captor.getValue();
        assertEquals(2, chatGPTMessages.size());
        assertTrue(chatGPTMessages.stream().anyMatch(chatGPTMessage -> expectedRequestText.equals(chatGPTMessage.getContent())));
        assertTrue(chatGPTMessages.stream().anyMatch(chatGPTMessage -> ChatGPTRole.USER.equals(chatGPTMessage.getRole())));
        assertTrue(chatGPTMessages.stream().anyMatch(chatGPTMessage -> expectedResponseText.equals(chatGPTMessage.getContent())));
        assertTrue(chatGPTMessages.stream().anyMatch(chatGPTMessage -> ChatGPTRole.ASSISTANT.equals(chatGPTMessage.getRole())));

        TestUtils.checkDefaultSendMessageParams(sendMessage);
        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void messageFromUserWithHistoryTest() throws JsonProcessingException {
        final String expectedRequestText = "say hello";
        final String expectedResponseText = "hello";
        Update update = getUpdate(1L, "chatgpt " + expectedRequestText);

        List<ChatGPTMessage> chatGPTMessages = new ArrayList<>(
                List.of(new ChatGPTMessage().setRole(ChatGPTRole.USER).setUser(new User().setUsername("username"))));

        ChatGPT.Message message = new ChatGPT.Message();
        message.setContent(expectedResponseText);
        ChatGPT.Choice choice = new ChatGPT.Choice();
        choice.setMessage(message);
        ChatGPT.Response response = new ChatGPT.Response();
        response.setChoices(List.of(choice));

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(chatGPTMessageService.getMessages(any(User.class))).thenReturn(chatGPTMessages);
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        SendMessage sendMessage = chatGPT.parse(update);

        verify(chatGPTMessageService).update(captor.capture());
        List<ChatGPTMessage> actualChatGPTMessages = captor.getValue();
        assertEquals(3, actualChatGPTMessages.size());
        assertTrue(actualChatGPTMessages.stream().anyMatch(chatGPTMessage -> expectedRequestText.equals(chatGPTMessage.getContent())));
        assertTrue(actualChatGPTMessages.stream().anyMatch(chatGPTMessage -> ChatGPTRole.USER.equals(chatGPTMessage.getRole())));
        assertTrue(actualChatGPTMessages.stream().anyMatch(chatGPTMessage -> expectedResponseText.equals(chatGPTMessage.getContent())));
        assertTrue(actualChatGPTMessages.stream().anyMatch(chatGPTMessage -> ChatGPTRole.ASSISTANT.equals(chatGPTMessage.getRole())));

        TestUtils.checkDefaultSendMessageParams(sendMessage);

        assertEquals(expectedResponseText, sendMessage.getText());
    }

}