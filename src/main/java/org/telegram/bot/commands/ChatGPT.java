package org.telegram.bot.commands;

import com.drew.lang.annotations.NotNull;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import lombok.Data;
import lombok.Getter;
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
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.ChatGPTMessage;
import org.telegram.bot.domain.entities.ChatGPTSettings;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.ChatGPTRole;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.TextUtils;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.containsStartWith;
import static org.telegram.bot.utils.TextUtils.getStartsWith;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatGPT implements Command {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/";
    private static final String DEFAULT_MODEL = "gpt-5-mini";
    private static final String RESPONSE_CAPTION = "ChatGPT";

    @Value("${chatGptApiUrl}")
    private String chatGptApiUrl;

    private final Set<String> imageCommands = new HashSet<>();
    private final Encoding encoding = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

    private final Bot bot;
    private final PropertiesConfig propertiesConfig;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final ChatGPTMessageService chatGPTMessageService;
    private final ChatGPTSettingService chatGPTSettingService;
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
    public List<BotResponse> parse(BotRequest request) {
        String token = propertiesConfig.getChatGPTToken();
        if (StringUtils.isEmpty(token)) {
            log.error("Unable to find google token");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        org.telegram.bot.domain.model.request.Message message = request.getMessage();
        Long chatId = message.getChatId();

        String commandArgument = commandWaitingService.getText(message);

        Response response;
        if (commandArgument != null) {
            String lowerTextMessage = commandArgument.toLowerCase(Locale.ROOT);
            if (containsStartWith(imageCommands, lowerTextMessage)) {
                bot.sendUploadPhoto(chatId);
                response = getImageResponse(commandArgument, lowerTextMessage, token);
            } else {
                bot.sendTyping(chatId);
                response = getTextResponse(message, commandArgument, token);
            }
        } else {
            bot.sendTyping(chatId);
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());
            response = new Response("${command.chatgpt.commandwaitingstart}", null);
        }

        if (response.getImageUrl() != null) {
            return returnResponse(new FileResponse(message)
                    .setText(response.getResponseText())
                    .addFile(new File(FileType.IMAGE, response.getImageUrl()))
                    .setResponseSettings(FormattingStyle.MARKDOWN));
        }

        return returnResponse(new TextResponse(message)
                .setText(buildResponseText(response))
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }

    private String buildResponseText(Response response) {
        String responseText = response.getResponseText();

        String model = response.getModel();
        if (model != null && !model.isEmpty()) {
            return "*" + RESPONSE_CAPTION + "* (" + model + "):\n" + responseText;
        }

        return responseText;
    }

    private Response getImageResponse(String commandArgument, String lowerTextMessage, String token) {
        String imageCommand = getStartsWith(
                internationalizationService.internationalize("${command.chatgpt.imagecommand}"),
                lowerTextMessage);
        if (imageCommand == null) {
            log.error("Unable to find image command in text {}", lowerTextMessage);
            botStats.incrementErrors(lowerTextMessage, "Unable to find image command in text");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        commandArgument = commandArgument.substring(imageCommand.length() + 1);
        String responseText = TextUtils.cutIfLongerThan(commandArgument, 1000);

        CreateImageResponse createImageResponse = getResponse(new CreateImageRequest().setPrompt(commandArgument), token);

        Optional<String> imageUrl = Optional.of(createImageResponse)
                .map(CreateImageResponse::getData)
                .filter(imageUrls -> !imageUrls.isEmpty())
                .map(imageUrls -> imageUrls.get(0))
                .map(ImageUrl::getUrl);

        if (imageUrl.isEmpty()) {
            log.error("Unable to find response text inside the api-response");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return new Response(responseText, imageUrl.get(), createImageResponse.getModel());
    }

    private Response getTextResponse(org.telegram.bot.domain.model.request.Message message, String commandArgument, String token) {
        Chat chat = message.getChat();
        User user = message.getUser();

        List<ChatGPTMessage> messagesHistory;
        if (message.getChatId() < 0) {
            messagesHistory = chatGPTMessageService.getMessages(chat);
        } else {
            messagesHistory = chatGPTMessageService.getMessages(user);
        }

        messagesHistory = reduceToTokensSize(messagesHistory, commandArgument);

        ChatGPTSettings chatGPTSettings = chatGPTSettingService.get(chat);
        String model = getModel(chatGPTSettings);
        String prompt = getPrompt(chatGPTSettings);

        ChatResponse response = getResponse(
                buildRequest(messagesHistory, commandArgument, user.getUsername(), model, prompt),
                token);
        String responseText = getResponseText(response);
        String responseModel = response.getModel();

        messagesHistory.addAll(
                List.of(
                        new ChatGPTMessage().setChat(chat).setUser(user).setRole(ChatGPTRole.USER).setContent(commandArgument),
                        new ChatGPTMessage().setChat(chat).setUser(user).setRole(ChatGPTRole.ASSISTANT).setContent(responseText)));
        chatGPTMessageService.update(messagesHistory);

        return new Response(responseText, responseModel);
    }

    private List<ChatGPTMessage> reduceToTokensSize(List<ChatGPTMessage> messagesHistory, String newText) {
        Integer chatGPTTokensSize = propertiesConfig.getChatGPTTokensSize();
        if (chatGPTTokensSize == 0) {
            return messagesHistory;
        }

        int currentTokens = countTokens(messagesHistory, newText);
        while (currentTokens > chatGPTTokensSize && !messagesHistory.isEmpty()) {
            messagesHistory = chatGPTMessageService.update(messagesHistory, 2);
            currentTokens = countTokens(messagesHistory, newText);
        }

        if (messagesHistory.isEmpty() && currentTokens > chatGPTTokensSize) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.TOO_BIG_REQUEST));
        }

        return messagesHistory;
    }

    private int countTokens(List<ChatGPTMessage> messagesHistory, String newText) {
        List<String> content = messagesHistory.stream().map(ChatGPTMessage::getContent).collect(Collectors.toList());
        content.add(newText);

        return encoding.countTokens(String.join("", content));
    }

    @NotNull
    private String getModel(ChatGPTSettings chatGPTSettings) {
        if (chatGPTSettings != null) {
            String currentChatsModel = chatGPTSettings.getModel();
            if (currentChatsModel != null) {
                return currentChatsModel;
            }
        }

        List<String> chatGPTModelsAvailable = propertiesConfig.getChatGPTModelsAvailable();
        if (CollectionUtils.isEmpty(chatGPTModelsAvailable)) {
            return DEFAULT_MODEL;
        }

        return chatGPTModelsAvailable.get(0);
    }

    @Nullable
    private String getPrompt(ChatGPTSettings chatGPTSettings) {
        if (chatGPTSettings != null) {
            return chatGPTSettings.getPrompt();
        }

        return null;
    }

    private String getResponseText(ChatResponse chatResponse) {
        Optional<String> response = Optional.of(chatResponse)
                .map(ChatResponse::getChoices)
                .filter(choices -> !choices.isEmpty())
                .flatMap(choices -> choices
                        .stream()
                        .map(Choice::getMessage)
                        .map(Message::getContent)
                        .filter(org.springframework.util.StringUtils::hasLength)
                        .findFirst());

        if (response.isEmpty()) {
            log.error("Unable to find response text inside the api-response");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return response.get();
    }

    private ChatRequest buildRequest(List<ChatGPTMessage> chatGPTMessages, String text, String username, String model, String prompt) {
        List<Message> requestMessages = new ArrayList<>(chatGPTMessages.size() + 2);
        if (prompt != null && prompt.length() > 1) {
            requestMessages.add(new Message().setRole(ChatGPTRole.SYSTEM.getName()).setContent(prompt));
        }

        requestMessages.addAll(chatGPTMessages
                .stream()
                .map(chatGPTMessage -> new Message()
                        .setRole(chatGPTMessage.getRole().getName())
                        .setContent(chatGPTMessage.getContent())
                        .setName(chatGPTMessage.getUser().getUsername()))
                .toList());

        requestMessages.add(new Message().setRole(ChatGPTRole.USER.getName()).setContent(text).setName(username));

        return new ChatRequest().setModel(model).setMessages(requestMessages);
    }

    private CreateImageResponse getResponse(CreateImageRequest request, String token) {
        String url = chatGptApiUrl + "images/generations";
        return getResponse(request, url, token, CreateImageResponse.class);
    }

    private ChatResponse getResponse(ChatRequest request, String token) {
        String url = chatGptApiUrl + "chat/completions";
        return getResponse(request, url, token, ChatResponse.class);
    }

    private <T> T getResponse(Object request, String url, String token, Class<T> dataType) {
        String json;
        try {
            json = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            botStats.incrementErrors(request, e, "object serialization error");
            log.error("Failed to send request to ChatGPT API: {}", e.getMessage());
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
        } catch (Exception e) {
            log.error("Unknown error while trying to get response from chatgpt");
            botStats.incrementErrors(request, e, "Unknown error while trying to get response from chatgpt");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        T response = responseEntity.getBody();
        if (response == null) {
            log.error("Empty response from ChatGPT API");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return response;
    }

    @Getter
    private static class Response {
        private final String responseText;
        private final String imageUrl;
        private final String model;

        private Response(String responseText, String imageUrl, String model) {
            this.responseText = responseText;
            this.imageUrl = imageUrl;
            this.model = model;
        }

        private Response(String responseText, String model) {
            this.model = model;
            this.imageUrl = null;
            this.responseText = responseText;
        }
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
        private String model;
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
    @Accessors(chain = true)
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
