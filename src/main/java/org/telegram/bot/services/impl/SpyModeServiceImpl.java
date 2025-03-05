package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.services.SpyModeService;
import org.telegram.bot.utils.TextUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpyModeServiceImpl implements SpyModeService {

    private final PropertiesConfig propertiesConfig;

    private final Map<Long, LocalDateTime> userIdLastAlertDateTimeMap = new ConcurrentHashMap<>();

    private static final Integer SECONDS_WITHOUT_REPEAT_ALERTS = 30;

    @Override
    public TextResponse generateResponse(User user, String textMessage) {
        LocalDateTime dateTimeNow = LocalDateTime.now();
        Long userId = user.getUserId();

        ResponseSettings responseSettings = new ResponseSettings().setFormattingStyle(FormattingStyle.MARKDOWN);
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
                .setText("Received a message from " + TextUtils.getMarkdownLinkToUser(user) + ": `" + textMessage + "`")
                .setResponseSettings(responseSettings);
    }
}
