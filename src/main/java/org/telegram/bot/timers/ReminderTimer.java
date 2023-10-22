package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.bot.commands.Remind;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.services.ReminderService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.executors.SendMessageExecutor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderTimer extends TimerParent {

    private final ReminderService reminderService;
    private final UserCityService userCityService;
    private final LanguageResolver languageResolver;
    private final SendMessageExecutor sendMessageExecutor;

    @Override
    @Scheduled(fixedRate = 30000)
    public void execute() {
        Map<User, ZoneId> userDateTimeMap = new HashMap<>();
        LocalDateTime dateTimeNow = LocalDateTime.now();

        for (Reminder reminder : reminderService.getAllNotNotifiedByDate(dateTimeNow.toLocalDate())) {
            User user = reminder.getUser();
            Chat chat = reminder.getChat();

            LocalDateTime reminderDateTime = LocalDateTime.of(reminder.getDate(), reminder.getTime());
            ZoneId zoneId = getDateTimeOfUser(userDateTimeMap, chat, user);
            ZonedDateTime zonedDateTime = reminderDateTime.atZone(zoneId);

            if (dateTimeNow.isAfter(zonedDateTime.toLocalDateTime())) {
                Locale locale = languageResolver.getLocale(chat);

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chat.getChatId());
                sendMessage.enableHtml(true);
                sendMessage.disableWebPagePreview();
                sendMessage.setText(Remind.prepareTextOfReminder(reminder));
                sendMessage.setReplyMarkup(Remind.preparePostponeKeyboard(reminder, locale));

                sendMessageExecutor.executeMethod(sendMessage);

                String repeatability = reminder.getRepeatability();
                if (StringUtils.isEmpty(repeatability)) {
                    reminder.setNotified(true);
                } else {
                    LocalDateTime newReminderDateTime = reminderService.getNextAlarmDateTime(reminder);

                    reminder.setDate(newReminderDateTime.toLocalDate());
                    reminder.setTime(newReminderDateTime.toLocalTime());
                }

                reminderService.save(reminder);
            }
        }
    }

    private ZoneId getDateTimeOfUser(Map<User, ZoneId> userDateTimeMap, Chat chat, User user) {
        ZoneId zoneId = userDateTimeMap.get(user);
        if (zoneId == null) {
            zoneId = userCityService.getZoneIdOfUser(chat, user);

            if (zoneId == null) {
                zoneId = ZoneId.systemDefault();
            }

            userDateTimeMap.put(user, zoneId);
        }

        return zoneId;
    }
}
