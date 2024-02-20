package org.telegram.bot.commands;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatGPTMessage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.ChatGPTRole;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatGPTMessageService;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.containsStartWith;
import static org.telegram.bot.utils.TextUtils.getStartsWith;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatGPT implements Command<PartialBotApiMethod<?>> {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/";
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";

    @Value("${chatGptApiUrl}")
    private String chatGptApiUrl;

    private final Set<String> imageCommands = new HashSet<>();

    private final Bot bot;
    private final PropertiesConfig propertiesConfig;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final ChatGPTMessageService chatGPTMessageService;
    private final InternationalizationService internationalizationService;
    private final ObjectMapper objectMapper;
    private final RestTemplate defaultRestTemplate;
    private final BotStats botStats;

    @PostConstruct
    private void postConstruct() {
        if (chatGptApiUrl == null || chatGptApiUrl.isEmpty()) {
            chatGptApiUrl = OPENAI_API_URL;
        }

        imageCommands.addAll(internationalizationService.getAllTranslations("command.chatgpt.imagecommand"));
    }

    @Override
    public List<PartialBotApiMethod<?>> parse(Update update) {
        String token = propertiesConfig.getChatGPTToken();
        if (StringUtils.isEmpty(token)) {
            log.error("Unable to find google token");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        org.telegram.telegrambots.meta.api.objects.Message message = getMessageFromUpdate(update);
        Long chatId = message.getChatId();
        String textMessage = commandWaitingService.getText(message);
        String responseText;

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        String imageUrl = null;

        if (textMessage != null) {
            String lowerTextMessage = textMessage.toLowerCase();

            if (containsStartWith(imageCommands, lowerTextMessage)) {
                bot.sendUploadPhoto(chatId);

                String imageCommand = getStartsWith(
                        internationalizationService.internationalize("${command.chatgpt.imagecommand}"),
                        lowerTextMessage);
                if (imageCommand == null) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                }

                textMessage = textMessage.substring(imageCommand.length() + 1);
                responseText = TextUtils.cutIfLongerThan(textMessage, 1000);

                imageUrl = getResponse(new CreateImageRequest().setPrompt(textMessage), token);
            } else {
                bot.sendTyping(chatId);
                Chat chat = new Chat().setChatId(chatId);
                User user = new User().setUserId(message.getFrom().getId());
                List<ChatGPTMessage> messagesHistory;

                if (chatId < 0) {
                    messagesHistory = chatGPTMessageService.getMessages(chat);
                } else {
                    messagesHistory = chatGPTMessageService.getMessages(user);
                }

                responseText = getResponse(
                        buildRequest(messagesHistory, textMessage, message.getFrom().getUserName()),
                        token);

                messagesHistory.addAll(
                        List.of(
                                new ChatGPTMessage().setChat(chat).setUser(user).setRole(ChatGPTRole.USER).setContent(textMessage),
                                new ChatGPTMessage().setChat(chat).setUser(user).setRole(ChatGPTRole.ASSISTANT).setContent(responseText)));
                chatGPTMessageService.update(messagesHistory);
            }
        } else {
            bot.sendTyping(chatId);
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.chatgpt.commandwaitingstart}";
        }

        if (imageUrl != null) {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setPhoto(new InputFile(imageUrl));
            sendPhoto.setCaption(responseText);
            sendPhoto.setParseMode("HTML");
            sendPhoto.setReplyToMessageId(message.getMessageId());
            sendPhoto.setChatId(chatId);

            returnOneResult(sendPhoto);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);
        sendMessage.enableMarkdown(true);

        return returnOneResult(sendMessage);
    }

    private ChatRequest buildRequest(List<ChatGPTMessage> chatGPTMessages, String text, String username) {
        List<Message> requestMessages = chatGPTMessages
                .stream()
                .map(chatGPTMessage -> new Message()
                        .setRole(chatGPTMessage.getRole().getName())
                        .setContent(chatGPTMessage.getContent())
                        .setName(chatGPTMessage.getUser().getUsername()))
                .collect(Collectors.toList());
        requestMessages.add(new Message().setRole(ChatGPTRole.USER.getName()).setContent(text).setName(username));

        return new ChatRequest().setModel(DEFAULT_MODEL).setMessages(requestMessages);
    }

    private String getResponse(CreateImageRequest request, String token) {
        String url = chatGptApiUrl + "images/generations";
        CreateImageResponse response = getResponse(request, url, token, CreateImageResponse.class);
        return response.getData().get(0).getUrl();
    }

    private String getResponse(ChatRequest request, String token) {
        String url = chatGptApiUrl + "chat/completions";
        ChatResponse response = getResponse(request, url, token, ChatResponse.class);
        return response.getChoices().get(0).getMessage().getContent();
    }

    private <T> T getResponse(Object request, String url, String token, Class<T> dataType) {
        String json;
        try {
            json = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            botStats.incrementErrors(request, e, "object serialization error");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        ResponseEntity<T> responseEntity;
        try {
            responseEntity = defaultRestTemplate.postForEntity(url, new HttpEntity<>(json, headers), dataType);
        } catch (HttpClientErrorException hce) {
            String jsonError = hce.getResponseBodyAsString();

            ErrorResponse errorResponse;
            try {
                errorResponse = objectMapper.readValue(jsonError, ErrorResponse.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to map {} to Error", jsonError);
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
            }

            throw new BotException("${command.chatgpt.apiresponse}: " + errorResponse.getError().getMessage());
        } catch (RestClientException e) {
            log.error("Error from chatgpt: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        T response = responseEntity.getBody();
        if (response == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return response;
    }

    @Data
    @Accessors(chain = true)
    public static class ErrorResponse {
        private Error error;
    }

    @Data
    @Accessors(chain = true)
    public static class Error {
        private String message;
        private String type;
        private String param;
        private String code;
    }

    @Data
    @Accessors(chain = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreateImageRequest {
        private String prompt;
        private Integer n;
        private String size;
    }

    @Data
    public static class CreateImageResponse {
        private Integer created;
        private List<ImageUrl> data;
    }

    @Data
    public static class ImageUrl {
        String url;
    }

    @Data
    @Accessors(chain = true)
    public static class ChatRequest {
        private String model;
        private List<Message> messages;
        private Float temperature;
    }

    @Data
    public static class ChatResponse {
        private String id;
        private String object;
        private Integer created;
        private String model;
        private Usage usage;
        private List<Choice> choices;
    }

    @Data
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    @Data
    public static class Choice {
        private Message message;

        @JsonProperty("finish_reason")
        private String finishReason;

        private Integer index;
    }

    @Data
    @Accessors(chain = true)
    public static class Message {
        private String role;
        private String content;
        private String name;
    }
}
