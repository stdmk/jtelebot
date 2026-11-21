package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.services.EmailNotifier;
import org.telegram.bot.services.LogService;
import org.telegram.bot.utils.TextUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogServiceImpl implements LogService {

    private static final Integer SECONDS_WITHOUT_REPEAT_ALERTS = 30;

    private final Map<Long, LocalDateTime> userIdLastAlertDateTimeMap = new ConcurrentHashMap<>();

    private final PropertiesConfig propertiesConfig;
    private final Bot bot;
    private final EmailNotifier emailNotifier;

    @Override
    public void log(BotRequest botRequest) {
        Message message = botRequest.getMessage();
        org.telegram.bot.domain.entities.User user = message.getUser();

        String textOfMessage = getText(message);
        Long chatId = message.getChatId();
        Long userId = user.getUserId();
        log.info("({}) From {} ({}-{}): {}", botRequest.getSource().getName(), chatId, user.getUsername(), userId, textOfMessage);

        Boolean spyMode = propertiesConfig.getSpyMode();
        if (chatId > 0 && spyMode != null && spyMode) {
            reportToAdmin(user, textOfMessage);
        }

        emailNotifier.notify(botRequest);
    }

    private String getText(Message message) {
        String name = Optional.of(message)
                .map(Message::getAttachments)
                .filter(attachments -> !attachments.isEmpty())
                .map(List::getFirst)
                .map(Attachment::getName)
                .orElse("");

        switch (message.getMessageContentType()) {
            case TEXT -> {
                return message.getText();
            }
            case STICKER -> {
                return "sticker" + name;
            }
            case PHOTO -> {
                return "photo" + name;
            }
            case ANIMATION -> {
                return "animation" + name;
            }
            case AUDIO -> {
                return "audio" + name;
            }
            case FILE -> {
                return "file" + name;
            }
            case VIDEO -> {
                return "video" + name;
            }
            case VIDEO_NOTE -> {
                return "video note" + name;
            }
            case VOICE -> {
                return "voice" + name;
            }
            case REACTION -> {
                return message.getReactions().toString();
            }
            default -> {
                return null;
            }
        }
    }

    private void reportToAdmin(org.telegram.bot.domain.entities.User user, String textMessage) {
        Long adminId = propertiesConfig.getAdminId();
        if (adminId.equals(user.getUserId()) || bot.getBotUsername().equals(user.getUsername())) {
            return;
        }

        TextResponse textResponse = this.generateResponse(user, textMessage);

        bot.sendMessage(textResponse);
    }

    private TextResponse generateResponse(User user, String textMessage) {
        LocalDateTime dateTimeNow = LocalDateTime.now();
        Long userId = user.getUserId();

        ResponseSettings responseSettings = new ResponseSettings().setFormattingStyle(FormattingStyle.HTML);
        if (userIdLastAlertDateTimeMap.containsKey(userId)) {
            LocalDateTime lastAlertDateTime = userIdLastAlertDateTimeMap.get(userId);
            if (lastAlertDateTime.plusSeconds(SECONDS_WITHOUT_REPEAT_ALERTS).isBefore(dateTimeNow)) {
                userIdLastAlertDateTimeMap.put(userId, dateTimeNow);
            } else {
                responseSettings.setNotification(false);
            }
        } else {
            userIdLastAlertDateTimeMap.put(userId, dateTimeNow);
        }

        return new TextResponse()
                .setChatId(propertiesConfig.getAdminId())
                .setText("Received a message from " + TextUtils.getHtmlLinkToUser(user) + ": <code>" + textMessage + "</code>")
                .setResponseSettings(responseSettings);
    }

}
