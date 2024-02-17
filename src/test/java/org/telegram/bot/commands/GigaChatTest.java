package org.telegram.bot.commands;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.GigaChatMessage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.GigaChatRole;
import org.telegram.bot.enums.SberScope;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.GettingSberAccessTokenException;
import org.telegram.bot.providers.sber.SberTokenProvider;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.GigaChatMessageService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.telegram.bot.TestUtils.*;

@ExtendWith(MockitoExtension.class)
class GigaChatTest {

    @Mock
    private Bot bot;
    @Mock
    private SberTokenProvider sberTokenProvider;
    @Mock
    private SpeechService speechService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private GigaChatMessageService gigaChatMessageService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private RestTemplate sberRestTemplate;
    @Mock
    private BotStats botStats;

    @Captor
    ArgumentCaptor<List<GigaChatMessage>> captor;

    @InjectMocks
    private GigaChat gigaChat;

    @Test
    void unavailableTokenTest() throws GettingSberAccessTokenException {
        Update update = getUpdateFromGroup();
        when(sberTokenProvider.getToken(SberScope.GIGACHAT_API_PERS)).thenThrow(new GettingSberAccessTokenException("error"));

        assertThrows(BotException.class, () -> gigaChat.parse(update));
        verify(bot, never()).sendTyping(update.getMessage().getChatId());
        verify(bot, never()).sendUploadPhoto(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void emptyParamTest() throws GettingSberAccessTokenException {
        Update update = getUpdateFromGroup();
        when(sberTokenProvider.getToken(SberScope.GIGACHAT_API_PERS)).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);

        gigaChat.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(commandWaitingService).add(any(Message.class), any(Class.class));
    }

    @Test
    void requestSerialisationError() throws JsonProcessingException, GettingSberAccessTokenException {
        Update update = getUpdateFromGroup("gigachat say hello");

        when(sberTokenProvider.getToken(SberScope.GIGACHAT_API_PERS)).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(gigaChatMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenThrow(mock(JsonProcessingException.class));

        assertThrows(BotException.class, () -> gigaChat.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(botStats).incrementErrors(any(Object.class), any(JsonProcessingException.class), anyString());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void requestWithRestClientExceptionError() throws JsonProcessingException, GettingSberAccessTokenException {
        Update update = getUpdateFromGroup("gigachat say hello");

        when(sberTokenProvider.getToken(SberScope.GIGACHAT_API_PERS)).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(gigaChatMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any())).thenThrow(new RestClientException("test"));

        assertThrows(BotException.class, () -> gigaChat.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void requestWithHttpClientErrorExceptionTest() throws JsonProcessingException, GettingSberAccessTokenException {
        final String errorMessage = "error";
        final String expectedErrorText = "Ответ от GigaChat: " + errorMessage;
        GigaChat.ErrorResponse errorResponse = new GigaChat.ErrorResponse()
                .setError(new GigaChat.Error()
                        .setCode("")
                        .setType("")
                        .setParam("")
                        .setMessage(errorMessage));
        Update update = getUpdateFromGroup("gigachat say hello");

        when(sberTokenProvider.getToken(SberScope.GIGACHAT_API_PERS)).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(gigaChatMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(objectMapper.readValue("", GigaChat.ErrorResponse.class)).thenReturn(errorResponse);
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "", "".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

        BotException botException = assertThrows(BotException.class, () -> gigaChat.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void requestWithHttpClientErrorExceptionWithCorruptedErrorTest() throws JsonProcessingException, GettingSberAccessTokenException {
        Update update = getUpdateFromGroup("gigachat say hello");

        when(sberTokenProvider.getToken(SberScope.GIGACHAT_API_PERS)).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(gigaChatMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(objectMapper.readValue(anyString(), ArgumentMatchers.<Class<?>>any())).thenThrow(new JsonParseException(null, ""));
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "", "error".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

        assertThrows(BotException.class, () -> gigaChat.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void requestWithEmptyResponseTest() throws JsonProcessingException, GettingSberAccessTokenException {
        Update update = getUpdateFromGroup("gigachat say hello");

        when(sberTokenProvider.getToken(SberScope.GIGACHAT_API_PERS)).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(gigaChatMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertThrows(BotException.class, () -> gigaChat.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void requestWithNoTextInResponseTest() throws JsonProcessingException, GettingSberAccessTokenException {
        final String expectedResponseText = "\n";
        Update update = getUpdateFromGroup("gigachat say hello");
        GigaChat.Message message = new GigaChat.Message();
        message.setContent(expectedResponseText);
        GigaChat.Choice choice = new GigaChat.Choice();
        choice.setMessage(message);
        GigaChat.ChatResponse response = new GigaChat.ChatResponse();
        response.setChoices(List.of(choice));

        when(sberTokenProvider.getToken(SberScope.GIGACHAT_API_PERS)).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(gigaChatMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        assertThrows(BotException.class, () -> gigaChat.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void requestWithImgInResponse() throws GettingSberAccessTokenException, JsonProcessingException {
        final String expectedResponseText = "hello";
        Update update = getUpdateFromGroup("gigachat picture cat");

        GigaChat.Message message = new GigaChat.Message();
        message.setContent(expectedResponseText + "<img src=\"abv\">");
        GigaChat.Choice choice = new GigaChat.Choice();
        choice.setMessage(message);
        GigaChat.ChatResponse response = new GigaChat.ChatResponse();
        response.setChoices(List.of(choice));

        when(sberTokenProvider.getToken(SberScope.GIGACHAT_API_PERS)).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(gigaChatMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
        when(sberRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), ArgumentMatchers.<Class<byte[]>>any()))
                .thenReturn(new ResponseEntity<>("response".getBytes(StandardCharsets.UTF_8), HttpStatus.OK));

        PartialBotApiMethod<?> method = gigaChat.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        SendPhoto sendPhoto = checkDefaultSendPhotoParams(method);
        assertEquals(expectedResponseText, sendPhoto.getCaption());

        verify(bot).sendUploadPhoto(DEFAULT_CHAT_ID);
    }

    @Test
    void messageFromChatWithEmptyHistoryTest() throws JsonProcessingException, GettingSberAccessTokenException {
        final String expectedRequestText = "say hello";
        final String expectedResponseText = "hello";
        Update update = getUpdateFromGroup("gigachat " + expectedRequestText);

        GigaChat.Message message = new GigaChat.Message();
        message.setContent(expectedResponseText);
        GigaChat.Choice choice = new GigaChat.Choice();
        choice.setMessage(message);
        GigaChat.ChatResponse response = new GigaChat.ChatResponse();
        response.setChoices(List.of(choice));

        when(sberTokenProvider.getToken(SberScope.GIGACHAT_API_PERS)).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(gigaChatMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        PartialBotApiMethod<?> method = gigaChat.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        SendMessage sendMessage = checkDefaultSendMessageParams(method);

        verify(gigaChatMessageService).update(captor.capture());
        List<GigaChatMessage> gigaChatMessages = captor.getValue();
        assertEquals(2, gigaChatMessages.size());
        assertTrue(gigaChatMessages.stream().anyMatch(gigaChatMessage -> expectedRequestText.equals(gigaChatMessage.getContent())));
        assertTrue(gigaChatMessages.stream().anyMatch(gigaChatMessage -> GigaChatRole.USER.equals(gigaChatMessage.getRole())));
        assertTrue(gigaChatMessages.stream().anyMatch(gigaChatMessage -> expectedResponseText.equals(gigaChatMessage.getContent())));
        assertTrue(gigaChatMessages.stream().anyMatch(gigaChatMessage -> GigaChatRole.ASSISTANT.equals(gigaChatMessage.getRole())));

        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void messageFromUserWithHistoryTest() throws JsonProcessingException, GettingSberAccessTokenException {
        final String expectedRequestText = "say hello";
        final String expectedResponseText = "hello";
        Update update = getUpdateFromPrivate("gigachat " + expectedRequestText);

        List<GigaChatMessage> gigaChatMessages = new ArrayList<>(
                List.of(new GigaChatMessage().setRole(GigaChatRole.USER).setUser(new User().setUsername("username"))));

        GigaChat.Message message = new GigaChat.Message();
        message.setContent(expectedResponseText);
        GigaChat.Choice choice = new GigaChat.Choice();
        choice.setMessage(message);
        GigaChat.ChatResponse response = new GigaChat.ChatResponse();
        response.setChoices(List.of(choice));

        when(sberTokenProvider.getToken(SberScope.GIGACHAT_API_PERS)).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(gigaChatMessageService.getMessages(any(User.class))).thenReturn(gigaChatMessages);
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(sberRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        PartialBotApiMethod<?> method = gigaChat.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        SendMessage sendMessage = checkDefaultSendMessageParams(method);

        verify(gigaChatMessageService).update(captor.capture());
        List<GigaChatMessage> actualGigaChatMessages = captor.getValue();
        assertEquals(3, actualGigaChatMessages.size());
        assertTrue(actualGigaChatMessages.stream().anyMatch(gigaChatMessage -> expectedRequestText.equals(gigaChatMessage.getContent())));
        assertTrue(actualGigaChatMessages.stream().anyMatch(gigaChatMessage -> GigaChatRole.USER.equals(gigaChatMessage.getRole())));
        assertTrue(actualGigaChatMessages.stream().anyMatch(gigaChatMessage -> expectedResponseText.equals(gigaChatMessage.getContent())));
        assertTrue(actualGigaChatMessages.stream().anyMatch(gigaChatMessage -> GigaChatRole.ASSISTANT.equals(gigaChatMessage.getRole())));

        assertEquals(expectedResponseText, sendMessage.getText());
    }

}