package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.commands.Remind;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.enums.ReminderRepeatability;
import org.telegram.bot.services.ReminderService;
import org.telegram.bot.services.UserCityService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.*;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderTimer extends TimerParent {

    private final ReminderService reminderService;
    private final UserCityService userCityService;
    private final Bot bot;

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
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chat.getChatId());
                sendMessage.enableHtml(true);
                sendMessage.setText(Remind.prepareTextOfReminder(reminder));
                sendMessage.setReplyMarkup(Remind.preparePostponeKeyboard(reminder));

                try {
                    bot.execute(sendMessage);
                } catch (TelegramApiException e) {
                    continue;
                }

                ReminderRepeatability repeatability = reminder.getRepeatability();
                if (repeatability != null) {
                    TemporalAmount temporalAmount = repeatability.getTemporalAmountSupplier().get();
                    LocalDateTime newReminderDateTime = reminder.getDate().atTime(reminder.getTime()).plus(temporalAmount);

                    reminder.setDate(newReminderDateTime.toLocalDate());
                    reminder.setTime(newReminderDateTime.toLocalTime());
                } else {
                    reminder.setNotified(true);
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
