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
import org.telegram.bot.TestUtils;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatGPTMessage;
import org.telegram.bot.domain.entities.ChatGPTSettings;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.ChatGPTRole;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
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
    private PropertiesConfig propertiesConfig;
    @Mock
    private SpeechService speechService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private ChatGPTMessageService chatGPTMessageService;
    @Mock
    private ChatGPTSettingService chatGPTSettingService;
    @Mock
    private InternationalizationService internationalizationService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private RestTemplate defaultRestTemplate;
    @Mock
    private BotStats botStats;

    @Captor
    ArgumentCaptor<List<ChatGPTMessage>> captor;

    @InjectMocks
    private ChatGPT chatGPT;

    @Test
    void unavailableTokenTest() {
        BotRequest request = getRequestFromGroup();
        when(propertiesConfig.getChatGPTToken()).thenReturn(null);

        assertThrows(BotException.class, () -> chatGPT.parse(request));
        verify(bot, never()).sendTyping(request.getMessage().getChatId());
        verify(bot, never()).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN);
    }

    @Test
    void emptyParamTest() {
        BotRequest request = getRequestFromGroup();
        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(any(Message.class))).thenReturn(null);

        chatGPT.parse(request);

        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(commandWaitingService).add(any(Message.class), any(Class.class));
    }

    @Test
    void requestSerialisationError() throws JsonProcessingException {
        BotRequest request = getRequestFromGroup("chatgpt say hello");

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenThrow(mock(JsonProcessingException.class));

        assertThrows(BotException.class, () -> chatGPT.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(botStats).incrementErrors(any(Object.class), any(JsonProcessingException.class), anyString());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void requestWithRestClientExceptionError() throws JsonProcessingException {
        BotRequest request = getRequestFromGroup("chatgpt say hello");

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any())).thenThrow(new RestClientException("test"));

        assertThrows(BotException.class, () -> chatGPT.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void requestWithHttpClientErrorExceptionTest() throws JsonProcessingException {
        final String errorMessage = "error";
        final String expectedErrorText = "${command.chatgpt.apiresponse}: " + errorMessage;
        ChatGPT.ErrorResponse errorResponse = new ChatGPT.ErrorResponse()
                .setError(new ChatGPT.Error()
                        .setCode("")
                        .setType("")
                        .setParam("")
                        .setMessage(errorMessage));
        BotRequest request = getRequestFromGroup("chatgpt say hello");

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(propertiesConfig.getChatGPTContextSize()).thenReturn(4);
        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(objectMapper.readValue("", ChatGPT.ErrorResponse.class)).thenReturn(errorResponse);
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "", "".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

        BotException botException = assertThrows(BotException.class, () -> chatGPT.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void requestWithHttpClientErrorExceptionWithCorruptedErrorTest() throws JsonProcessingException {
        BotRequest request = getRequestFromGroup("chatgpt say hello");

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(objectMapper.readValue(anyString(), ArgumentMatchers.<Class<?>>any())).thenThrow(new JsonParseException(null, ""));
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "", "error".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

        assertThrows(BotException.class, () -> chatGPT.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void requestWithHttpClientErrorExceptionAndRepeatedRequestTest() throws JsonProcessingException {
        final String errorMessage = "error";
        final String expectedErrorText = "${command.chatgpt.apiresponse}: " + errorMessage;
        ChatGPT.ErrorResponse errorResponse = new ChatGPT.ErrorResponse()
                .setError(new ChatGPT.Error()
                        .setCode("")
                        .setType("")
                        .setParam("")
                        .setMessage(errorMessage));
        BotRequest request = getRequestFromGroup("chatgpt say hello");

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(propertiesConfig.getChatGPTContextSize()).thenReturn(4);
        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(getSomeMessageHistory(4));
        when(chatGPTMessageService.update(anyList(), anyInt())).thenReturn(getSomeMessageHistory(2));
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(objectMapper.readValue("", ChatGPT.ErrorResponse.class)).thenReturn(errorResponse);
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "", "".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

        BotException botException = assertThrows(BotException.class, () -> chatGPT.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void requestWithEmptyResponseTest() throws JsonProcessingException {
        BotRequest request = getRequestFromGroup("chatgpt say hello");

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(chatGPTSettingService.get(request.getMessage().getChat())).thenReturn(new ChatGPTSettings().setModel("model"));
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<?>>any()))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertThrows(BotException.class, () -> chatGPT.parse(request));
        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void messageFromChatWithEmptyHistoryTest() throws JsonProcessingException {
        final String expectedModel = "model";
        final String requestText = "say hello";
        final String responseText = "hello.";
        final String expectedResponseText = "*ChatGPT* (" + expectedModel + "):\n" + responseText;
        BotRequest request = getRequestFromGroup("chatgpt " + requestText);

        ChatGPT.Message message = new ChatGPT.Message();
        message.setContent(responseText);
        ChatGPT.Choice choice = new ChatGPT.Choice();
        choice.setMessage(message);
        ChatGPT.ChatResponse response = new ChatGPT.ChatResponse();
        response.setChoices(List.of(choice)).setModel(expectedModel);

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(chatGPTMessageService.getMessages(any(Chat.class))).thenReturn(new ArrayList<>());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        BotResponse botResponse = chatGPT.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());
        TextResponse textResponse = checkDefaultTextResponseParams(botResponse);

        verify(chatGPTMessageService).update(captor.capture());
        List<ChatGPTMessage> chatGPTMessages = captor.getValue();
        assertEquals(2, chatGPTMessages.size());
        assertTrue(chatGPTMessages.stream().anyMatch(chatGPTMessage -> requestText.equals(chatGPTMessage.getContent())));
        assertTrue(chatGPTMessages.stream().anyMatch(chatGPTMessage -> ChatGPTRole.USER.equals(chatGPTMessage.getRole())));
        assertTrue(chatGPTMessages.stream().anyMatch(chatGPTMessage -> expectedResponseText.equals(chatGPTMessage.getContent())));
        assertTrue(chatGPTMessages.stream().anyMatch(chatGPTMessage -> ChatGPTRole.ASSISTANT.equals(chatGPTMessage.getRole())));

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void messageFromUserWithHistoryTest() throws JsonProcessingException {
        final String expectedModel = "model";
        final String requestText = "say hello";
        final String responseText = "hello.";
        final String expectedResponseText = "*ChatGPT* (" + expectedModel + "):\n" + responseText;
        BotRequest request = getRequestFromPrivate("chatgpt " + requestText);

        List<ChatGPTMessage> chatGPTMessages = new ArrayList<>(
                List.of(new ChatGPTMessage().setRole(ChatGPTRole.USER).setUser(new User().setUsername("username"))));

        ChatGPT.Message message = new ChatGPT.Message();
        message.setContent(responseText);
        ChatGPT.Choice choice = new ChatGPT.Choice();
        choice.setMessage(message);
        ChatGPT.ChatResponse response = new ChatGPT.ChatResponse();
        response.setChoices(List.of(choice)).setModel(expectedModel);

        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(chatGPTMessageService.getMessages(any(User.class))).thenReturn(chatGPTMessages);
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        BotResponse botResponse = chatGPT.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());
        TextResponse textResponse = checkDefaultTextResponseParams(botResponse);

        verify(chatGPTMessageService).update(captor.capture());
        List<ChatGPTMessage> actualChatGPTMessages = captor.getValue();
        assertEquals(3, actualChatGPTMessages.size());
        assertTrue(actualChatGPTMessages.stream().anyMatch(chatGPTMessage -> requestText.equals(chatGPTMessage.getContent())));
        assertTrue(actualChatGPTMessages.stream().anyMatch(chatGPTMessage -> ChatGPTRole.USER.equals(chatGPTMessage.getRole())));
        assertTrue(actualChatGPTMessages.stream().anyMatch(chatGPTMessage -> expectedResponseText.equals(chatGPTMessage.getContent())));
        assertTrue(actualChatGPTMessages.stream().anyMatch(chatGPTMessage -> ChatGPTRole.ASSISTANT.equals(chatGPTMessage.getRole())));

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void messageFromChatWithChatGptApiExceptionTest() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final String expectedUrl = "url";
        final String errorMessage = "error";
        final String expectedErrorText = "${command.chatgpt.apiresponse}: " + errorMessage;
        BotRequest request = getRequestFromGroup("chatgpt iMaGe say hello");

        ChatGPT.ImageUrl imageUrl = new ChatGPT.ImageUrl();
        imageUrl.setUrl(expectedUrl);
        ChatGPT.CreateImageResponse response = new ChatGPT.CreateImageResponse();
        response.setCreated(1);
        response.setData(List.of(imageUrl));

        ChatGPT.ErrorResponse errorResponse = new ChatGPT.ErrorResponse()
                .setError(new ChatGPT.Error()
                        .setCode("")
                        .setType("")
                        .setParam("")
                        .setMessage(errorMessage));

        Set<String> imageCommands = Set.of("image");
        when(internationalizationService.getAllTranslations("command.chatgpt.imagecommand")).thenReturn(imageCommands);
        when(internationalizationService.internationalize("${command.chatgpt.imagecommand}")).thenReturn(imageCommands);
        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(objectMapper.readValue("error", ChatGPT.ErrorResponse.class)).thenReturn(errorResponse);
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "", "error".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

        Method postConstruct = ChatGPT.class.getDeclaredMethod("postConstruct");
        postConstruct.setAccessible(true);
        postConstruct.invoke(chatGPT);

        BotException botException = assertThrows(BotException.class, () -> chatGPT.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
    }

    @Test
    void messageFromChatWithCreateImageRequestTest() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final String expectedUrl = "url";
        BotRequest request = getRequestFromGroup("chatgpt iMaGe say hello");

        ChatGPT.ImageUrl imageUrl = new ChatGPT.ImageUrl();
        imageUrl.setUrl(expectedUrl);
        ChatGPT.CreateImageResponse response = new ChatGPT.CreateImageResponse();
        response.setCreated(1);
        response.setData(List.of(imageUrl));

        Set<String> imageCommands = Set.of("image");
        when(internationalizationService.getAllTranslations("command.chatgpt.imagecommand")).thenReturn(imageCommands);
        when(internationalizationService.internationalize("${command.chatgpt.imagecommand}")).thenReturn(imageCommands);
        when(propertiesConfig.getChatGPTToken()).thenReturn("token");
        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());
        when(objectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        Method postConstruct = ChatGPT.class.getDeclaredMethod("postConstruct");
        postConstruct.setAccessible(true);
        postConstruct.invoke(chatGPT);

        BotResponse botResponse = chatGPT.parse(request).get(0);
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        FileResponse fileResponse = checkDefaultFileResponseImageParams(botResponse);

        assertEquals(expectedUrl, fileResponse.getFiles().get(0).getUrl());
    }

    @ParameterizedTest
    @MethodSource("provideApiUrls")
    void postConstructEmptyParamTest(String inputValue, String expectedUrl) throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InvocationTargetException {
        Field chatGptApiUrl = ChatGPT.class.getDeclaredField("chatGptApiUrl");
        chatGptApiUrl.setAccessible(true);
        chatGptApiUrl.set(chatGPT, inputValue);

        when(internationalizationService.getAllTranslations("command.chatgpt.imagecommand")).thenReturn(Set.of("image"));

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

    private List<ChatGPTMessage> getSomeMessageHistory(int size) {
        return IntStream.range(0, size).mapToObj(i -> getSomeChatGptMessage()).toList();
    }

    private ChatGPTMessage getSomeChatGptMessage() {
        return new ChatGPTMessage()
                .setId(1L)
                .setChat(TestUtils.getChat())
                .setUser(TestUtils.getUser())
                .setRole(ChatGPTRole.USER)
                .setContent("content");
    }

}