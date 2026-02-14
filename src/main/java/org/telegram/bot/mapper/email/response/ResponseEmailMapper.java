package org.telegram.bot.mapper.email.response;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.UserEmail;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.EmailResponse;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.utils.DateUtils;
import org.telegram.bot.utils.TextUtils;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ResponseEmailMapper {

    private final List<EmailResponseMapper> mappers;
    private final FileMapper fileMapper;
    private final ChatService chatService;
    @Lazy
    private final Bot bot;

    private final Map<Class<?>, EmailResponseMapper> mapperMap = new ConcurrentHashMap<>();

    @PostConstruct
    private void postConstruct() {
        mappers.forEach(mapper -> mapperMap.put(mapper.getMappingClass(), mapper));
    }

    public List<EmailResponse> toEmailResponse(List<BotResponse> botResponses, List<UserEmail> usersEmails) {
        Set<String> addresses = toAddresses(usersEmails);

        return botResponses
                .stream()
                .map(botResponse -> mapperMap.get(botResponse.getClass()).map(botResponse)
                        .setEmailAddresses(addresses)
                        .setSubject(buildSubject(botResponse.getChatId())))
                .map(emailResponse -> emailResponse.setText(getText(emailResponse.getText())))
                .toList();
    }

    public EmailResponse toEmailResponse(BotResponse botResponse, List<UserEmail> usersEmails) {
        EmailResponse emailResponse = mapperMap.get(botResponse.getClass()).map(botResponse)
                .setEmailAddresses(toAddresses(usersEmails))
                .setSubject(buildSubject(botResponse.getChatId()));

        emailResponse.setText(getText(emailResponse.getText()));

        return emailResponse;
    }

    public EmailResponse toEmailResponse(BotRequest botRequest, List<UserEmail> usersEmails) {
        Message message = botRequest.getMessage();
        if (message != null) {
            List<File> attachments = fileMapper.toFiles(message.getAttachments());

            String additional;
            if (attachments != null) {
                additional = attachments.stream().map(File::getText).filter(Objects::nonNull).collect(Collectors.joining("<br>"));
            } else {
                additional = "";
            }

            return new EmailResponse()
                    .setEmailAddresses(toAddresses(usersEmails))
                    .setSubject(buildSubject(message.getChatId()))
                    .setText(buildText(message, additional))
                    .setAttachments(attachments);
        }

        return null;
    }

    private Set<String> toAddresses(List<UserEmail> usersEmails) {
        return usersEmails.stream().map(UserEmail::getEmail).collect(Collectors.toSet());
    }

    private String buildSubject(Long chatId) {
        Chat chat = chatService.get(chatId);
        if (chat != null && chat.getName() != null) {
            return chat.getName() + " (" + chatId + ")";
        } else {
            return chatId.toString();
        }
    }

    private String getText(String text) {
        if (text == null) {
            text = "";
        }

        return "<u>" + DateUtils.formatDateTime(LocalDateTime.now()) + " <b>" + bot.getBotUsername() + "</b></u>:<br><br>"
                + text;
    }

    private String buildText(Message message, String additional) {
        String messageText;
        if (message.getText() == null) {
            messageText = additional;
        } else {
            messageText = message.getText() + "<br>" + additional;
        }

        return "<u>" + DateUtils.formatDateTime(message.getDateTime()) + " " + TextUtils.getHtmlLinkToUser(message.getUser()) + "</u>:<br><br>"
                + messageText;
    }

}
