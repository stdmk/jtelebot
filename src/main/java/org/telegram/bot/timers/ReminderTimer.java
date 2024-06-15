package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.commands.Remind;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.services.ReminderService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.services.LanguageResolver;

import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderTimer extends TimerParent {

    private static final ResponseSettings DEFAULT_RESPONSE_SETTINGS = new ResponseSettings()
            .setFormattingStyle(FormattingStyle.HTML)
            .setWebPagePreview(false);

    private final Bot bot;
    private final ReminderService reminderService;
    private final UserCityService userCityService;
    private final LanguageResolver languageResolver;

    private boolean isFirstExecute = true;

    @Override
    @Scheduled(fixedRate = 30000)
    public void execute() {
        Map<User, ZoneId> userDateTimeMap = new HashMap<>();
        LocalDateTime dateTimeNow = LocalDateTime.now();

        List<Reminder> notNotifiedRemindersList;
        if (isFirstExecute) {
            notNotifiedRemindersList = reminderService.getAllNotNotifiedBeforeDate(dateTimeNow.toLocalDate());
            isFirstExecute = false;
        } else {
            notNotifiedRemindersList = reminderService.getAllNotNotifiedByDate(dateTimeNow.toLocalDate());
        }

        ZonedDateTime zonedDateTimeNow = ZonedDateTime.now();
        for (Reminder reminder : notNotifiedRemindersList) {
            User user = reminder.getUser();
            Chat chat = reminder.getChat();

            LocalDateTime reminderDateTime = LocalDateTime.of(reminder.getDate(), reminder.getTime());
            ZoneId zoneId = getDateTimeOfUser(userDateTimeMap, chat, user);
            ZonedDateTime zonedDateTime = reminderDateTime.atZone(zoneId);

            if (zonedDateTimeNow.isAfter(zonedDateTime)) {
                Locale locale = languageResolver.getLocale(chat);

                bot.sendMessage(new TextResponse()
                        .setChatId(chat.getChatId())
                        .setText(Remind.prepareTextOfReminder(reminder))
                        .setKeyboard(Remind.preparePostponeKeyboard(reminder, zoneId, locale))
                        .setResponseSettings(DEFAULT_RESPONSE_SETTINGS));

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
