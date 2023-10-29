package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.services.SpyModeService;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpyModeServiceImpl implements SpyModeService {

    private final PropertiesConfig propertiesConfig;

    private final Map<Long, LocalDateTime> userIdLastAlertDateTimeMap = new ConcurrentHashMap<>();

    private static final Integer SECONDS_WITHOUT_REPEAT_ALERTS = 15;

    @Override
    public SendMessage generateMessage(User user, String textMessage) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(propertiesConfig.getAdminId().toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setText("Received a message from " + TextUtils.getLinkToUser(user, false) + ": `" + textMessage + "`");

        LocalDateTime dateTimeNow = LocalDateTime.now();
        Long userId = user.getId();
        if (userIdLastAlertDateTimeMap.containsKey(userId)) {
            LocalDateTime lastAlertDateTime = userIdLastAlertDateTimeMap.get(userId);
            if (lastAlertDateTime.plusSeconds(SECONDS_WITHOUT_REPEAT_ALERTS).isBefore(dateTimeNow)) {
                userIdLastAlertDateTimeMap.put(userId, dateTimeNow);
            } else {
                sendMessage.setDisableNotification(true);
            }
        } else {
            userIdLastAlertDateTimeMap.put(userId, dateTimeNow);
        }

        return sendMessage;
    }
}
