package org.telegram.bot.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.GigaChatMessage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.enums.GigaChatRole;
import org.telegram.bot.enums.SberScope;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.GettingSberAccessTokenException;
import org.telegram.bot.providers.sber.SberApiProvider;
import org.telegram.bot.providers.sber.SberTokenProvider;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.GigaChatMessageService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TextUtils;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GigaChat implements SberApiProvider, Command {

    private static final String GIGA_CHAT_API_URL = "https://gigachat.devices.sberbank.ru/api/v1/";
    private static final String COMPLETIONS_PATH = "chat/completions";
    private static final String FILES_PATH = "files/%s/content";
    private static final String DEFAULT_MODEL = "GigaChat:latest";
    private static final Pattern IMAGE_TAG_PATTERN = Pattern.compile("<img\\ssrc=\"([^\"]+)(?=\")");

    private final Bot bot;
    private final SberTokenProvider sberTokenProvider;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final GigaChatMessageService gigaChatMessageService;
    private final ObjectMapper objectMapper;
    private final RestTemplate sberRestTemplate;
    private final BotStats botStats;

    @Override
    public SberScope getScope() {
        return SberScope.GIGACHAT_API_PERS;
    }

    @Override
    public List<BotResponse> parse(BotRequest request) {
        String token;
        try {
            token = sberTokenProvider.getToken(getScope());
        } catch (GettingSberAccessTokenException e) {
            log.error("Failed to getting api access token of gigachat", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        org.telegram.bot.domain.model.request.Message message = request.getMessage();
        Long chatId = message.getChatId();
        String responseText;

        String commandArgument = commandWaitingService.getText(message);

        bot.sendTyping(chatId);
        byte[] image = null;
        if (commandArgument != null) {
            Chat chat = message.getChat();
            User user = message.getUser();
            List<GigaChatMessage> messagesHistory;

            if (chatId < 0) {
                messagesHistory = gigaChatMessageService.getMessages(chat);
            } else {
                messagesHistory = gigaChatMessageService.getMessages(user);
            }

            responseText = getResponse(
                    buildRequest(messagesHistory, commandArgument),
                    token);

            image = getImage(responseText, token);
            if (image != null) {
                bot.sendUploadPhoto(chatId);
                responseText = TextUtils.cutHtmlTags(responseText);
            }

            messagesHistory.addAll(
                    List.of(
                            new GigaChatMessage().setChat(chat).setUser(user).setRole(GigaChatRole.USER).setContent(commandArgument),
                            new GigaChatMessage().setChat(chat).setUser(user).setRole(GigaChatRole.ASSISTANT).setContent(responseText)));
            gigaChatMessageService.update(messagesHistory);
        } else {
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.gigachat.commandwaitingstart}";
        }

        if (image != null) {
            return returnResponse(new FileResponse(message)
                    .addFile(new File(FileType.IMAGE, new ByteArrayInputStream(image), "image"))
                    .setText(responseText)
                    .setResponseSettings(FormattingStyle.MARKDOWN));
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }

    private ChatRequest buildRequest(List<GigaChatMessage> gigaChatMessages, String text) {
        List<Message> requestMessages = gigaChatMessages
                .stream()
                .map(gigaChatMessage -> new Message()
                        .setRole(gigaChatMessage.getRole().getName())
                        .setContent(gigaChatMessage.getContent()))
                .collect(Collectors.toList());
        requestMessages.add(new Message().setRole(GigaChatRole.USER.getName()).setContent(text));

        return new ChatRequest().setModel(DEFAULT_MODEL).setMessages(requestMessages);
    }

    private String getResponse(ChatRequest request, String token) {
        String url = GIGA_CHAT_API_URL + COMPLETIONS_PATH;
        ChatResponse response = getResponse(request, url, token);

        return response.getChoices()
                .stream()
                .map(Choice::getMessage)
                .map(Message::getContent)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElseThrow(() -> new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE)));
    }

    private byte[] getImage(String text, String token) {
        Matcher matcher = IMAGE_TAG_PATTERN.matcher(text);
        if (matcher.find()) {
            return getFile(token, matcher.group(1));
        }

        return null;
    }

    private synchronized ChatResponse getResponse(Object request, String url, String token) {
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

        ResponseEntity<ChatResponse> responseEntity;
        try {
            responseEntity = sberRestTemplate.postForEntity(url, new HttpEntity<>(json, headers), ChatResponse.class);
        } catch (HttpClientErrorException hce) {
            String jsonError = hce.getResponseBodyAsString();

            ErrorResponse errorResponse;
            try {
                errorResponse = objectMapper.readValue(jsonError, ErrorResponse.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to map {} to Error", jsonError);
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
            }

            throw new BotException("Ответ от GigaChat: " + errorResponse.getError().getMessage());
        } catch (RestClientException e) {
            log.error("Error from gigachat: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        ChatResponse response = responseEntity.getBody();
        if (response == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return response;
    }

    private synchronized byte[] getFile(String token, String fileId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        ResponseEntity<byte[]> responseEntity;
        try {
            responseEntity = sberRestTemplate.exchange(
                    GIGA_CHAT_API_URL + String.format(FILES_PATH, fileId),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class);
        } catch (HttpClientErrorException hce) {
            String jsonError = hce.getResponseBodyAsString();

            ErrorResponse errorResponse;
            try {
                errorResponse = objectMapper.readValue(jsonError, ErrorResponse.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to map {} to Error", jsonError);
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
            }

            throw new BotException("Ответ от GigaChat: " + errorResponse.getError().getMessage());
        } catch (RestClientException e) {
            log.error("Error from gigachat: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        byte[] response = responseEntity.getBody();
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
    }
}
