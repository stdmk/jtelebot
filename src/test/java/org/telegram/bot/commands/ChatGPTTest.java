package org.telegram.bot.commands;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
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
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.telegram.bot.TestUtils.*;

@ExtendWith(MockitoExtension.class)
class ChatGPTTest {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/";

    @Mock
    private Bot bot;
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
        Update update = getUpdateFromGroup();
        when(propertiesConfig.getChatGPTToken()).thenReturn(null);

        assertThrows(BotException.class, () -> chatGPT.parse(update));
        verify(bot, never()).sendTyping(update.getMessage().getChatId());
        verify(bot, never()).sendUploadPhoto(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN);
    }

    @Test
    void emptyParamTest() {
        Update update = getUpdateFromGroup();
        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);

        chatGPT.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(commandWaitingService).add(any(Message.class), any(Class.class));
    }

    @Test
    void requestSerialisationError() throws JsonProcessingException {
        Update update = getUpdateFromGroup("chatgpt say hello");

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenThrow(mock(JsonProcessingException.class));

        assertThrows(BotException.class, () -> chatGPT.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(botStats).incrementErrors(any(Object.class), any(JsonProcessingException.class), anyString());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void requestWithRestClientExceptionError() throws JsonProcessingException {
        Update update = getUpdateFromGroup("chatgpt say hello");

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any())).thenThrow(new RestClientException("test"));

        assertThrows(BotException.class, () -> chatGPT.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void requestWithHttpClientErrorExceptionTest() throws JsonProcessingException {
        final String errorMessage = "error";
        final String expectedErrorText = "Ответ от ChatGPT: " + errorMessage;
        ChatGPT.ErrorResponse errorResponse = new ChatGPT.ErrorResponse()
                .setError(new ChatGPT.Error()
                        .setCode("")
                        .setType("")
                        .setParam("")
                        .setMessage(errorMessage));
        Update update = getUpdateFromGroup("chatgpt say hello");

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(objectMapper.readValue("", ChatGPT.ErrorResponse.class)).thenReturn(errorResponse);
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "", "".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

        BotException botException = assertThrows(BotException.class, () -> chatGPT.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void requestWithHttpClientErrorExceptionWithCorruptedErrorTest() throws JsonProcessingException {
        Update update = getUpdateFromGroup("chatgpt say hello");

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(objectMapper.readValue(anyString(), ArgumentMatchers.<Class<?>>any())).thenThrow(new JsonParseException(null, ""));
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "", "error".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

        assertThrows(BotException.class, () -> chatGPT.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void requestWithEmptyResponseTest() throws JsonProcessingException {
        Update update = getUpdateFromGroup("chatgpt say hello");

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertThrows(BotException.class, () -> chatGPT.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void messageFromChatWithEmptyHistoryTest() throws JsonProcessingException {
        final String expectedRequestText = "say hello";
        final String expectedResponseText = "hello";
        Update update = getUpdateFromGroup("chatgpt " + expectedRequestText);

        ChatGPT.Message message = new ChatGPT.Message();
        message.setContent(expectedResponseText);
        ChatGPT.Choice choice = new ChatGPT.Choice();
        choice.setMessage(message);
        ChatGPT.ChatResponse response = new ChatGPT.ChatResponse();
        response.setChoices(List.of(choice));

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        PartialBotApiMethod<?> method = chatGPT.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        SendMessage sendMessage = checkDefaultSendMessageParams(method);

        verify(chatGPTMessageService).update(captor.capture());
        List<ChatGPTMessage> chatGPTMessages = captor.getValue();
        assertEquals(2, chatGPTMessages.size());
        assertTrue(chatGPTMessages.stream().anyMatch(chatGPTMessage -> expectedRequestText.equals(chatGPTMessage.getContent())));
        assertTrue(chatGPTMessages.stream().anyMatch(chatGPTMessage -> ChatGPTRole.USER.equals(chatGPTMessage.getRole())));
        assertTrue(chatGPTMessages.stream().anyMatch(chatGPTMessage -> expectedResponseText.equals(chatGPTMessage.getContent())));
        assertTrue(chatGPTMessages.stream().anyMatch(chatGPTMessage -> ChatGPTRole.ASSISTANT.equals(chatGPTMessage.getRole())));

        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void messageFromUserWithHistoryTest() throws JsonProcessingException {
        final String expectedRequestText = "say hello";
        final String expectedResponseText = "hello";
        Update update = getUpdateFromPrivate("chatgpt " + expectedRequestText);

        List<ChatGPTMessage> chatGPTMessages = new ArrayList<>(
                List.of(new ChatGPTMessage().setRole(ChatGPTRole.USER).setUser(new User().setUsername("username"))));

        ChatGPT.Message message = new ChatGPT.Message();
        message.setContent(expectedResponseText);
        ChatGPT.Choice choice = new ChatGPT.Choice();
        choice.setMessage(message);
        ChatGPT.ChatResponse response = new ChatGPT.ChatResponse();
        response.setChoices(List.of(choice));

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(chatGPTMessageService.getMessages(any(User.class))).thenReturn(chatGPTMessages);
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        PartialBotApiMethod<?> method = chatGPT.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        SendMessage sendMessage = checkDefaultSendMessageParams(method);

        verify(chatGPTMessageService).update(captor.capture());
        List<ChatGPTMessage> actualChatGPTMessages = captor.getValue();
        assertEquals(3, actualChatGPTMessages.size());
        assertTrue(actualChatGPTMessages.stream().anyMatch(chatGPTMessage -> expectedRequestText.equals(chatGPTMessage.getContent())));
        assertTrue(actualChatGPTMessages.stream().anyMatch(chatGPTMessage -> ChatGPTRole.USER.equals(chatGPTMessage.getRole())));
        assertTrue(actualChatGPTMessages.stream().anyMatch(chatGPTMessage -> expectedResponseText.equals(chatGPTMessage.getContent())));
        assertTrue(actualChatGPTMessages.stream().anyMatch(chatGPTMessage -> ChatGPTRole.ASSISTANT.equals(chatGPTMessage.getRole())));

        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void messageFromChatWithCreateImageRequestTest() throws JsonProcessingException {
        final String expectedUrl = "url";
        Update update = getUpdateFromGroup("chatgpt iMaGe say hello");

        ChatGPT.ImageUrl imageUrl = new ChatGPT.ImageUrl();
        imageUrl.setUrl(expectedUrl);
        ChatGPT.CreateImageResponse response = new ChatGPT.CreateImageResponse();
        response.setCreated(1);
        response.setData(List.of(imageUrl));

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        PartialBotApiMethod<?> method = chatGPT.parse(update);
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        SendPhoto sendPhoto = checkDefaultSendPhotoParams(method);

        assertEquals(expectedUrl, sendPhoto.getPhoto().getAttachName());
    }

    @ParameterizedTest
    @MethodSource("provideApiUrls")
    void postConstructEmptyParamTest(String inputValue, String expectedUrl) throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InvocationTargetException {
        Field chatGptApiUrl = ChatGPT.class.getDeclaredField("chatGptApiUrl");
        chatGptApiUrl.setAccessible(true);
        chatGptApiUrl.set(chatGPT, inputValue);

        Method postConstruct = ChatGPT.class.getDeclaredMethod("postConstruct");
        postConstruct.setAccessible(true);

        postConstruct.invoke(chatGPT);

        assertEquals(expectedUrl, chatGptApiUrl.get(chatGPT));
    }

    private static Stream<Arguments> provideApiUrls() {
        return Stream.of(
                Arguments.of("", OPENAI_API_URL),
                Arguments.of("new_url", "new_url")
        );
    }

}