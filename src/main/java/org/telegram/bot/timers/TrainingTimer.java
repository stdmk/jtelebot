package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.DateUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrainingTimer extends TimerParent {

    private final Bot bot;
    private final TrainingEventService trainingEventService;
    private final TrainingScheduledService trainingScheduledService;
    private final TrainSubscriptionService trainSubscriptionService;
    private final TrainingStoppedService trainingStoppedService;
    private final UserCityService userCityService;

    private static final String COMMAND_NAME = "training";

    @Override
    @Scheduled(fixedRate = 600000)
    public void execute() {
        LocalDateTime dateTimeNow = LocalDateTime.now();
        DayOfWeek currentDayOfWeek = DayOfWeek.from(dateTimeNow);

        Map<User, TrainSubscription> userTrainSubscriptionMap = new HashMap<>();
        Map<User, ZoneId> userZoneIdMap = new HashMap<>();
        List<Long> pastTrainingIdList = trainingEventService.getAll(dateTimeNow.toLocalDate())
                .stream()
                .map(TrainingEvent::getTraining)
                .map(Training::getId)
                .collect(Collectors.toList());

        List<Long> userIdListWithStoppedTraining = trainingStoppedService.getAll()
                .stream()
                .map(TrainingStopped::getUser)
                .map(User::getUserId)
                .collect(Collectors.toList());

        trainingScheduledService.getAll(currentDayOfWeek)
                .stream()
                .filter(trainingScheduled -> !userIdListWithStoppedTraining.contains(trainingScheduled.getUser().getUserId()))
                .map(TrainingScheduled::getTraining)
                .filter(training -> !pastTrainingIdList.contains(training.getId()))
                .filter(training -> getUserTime(userZoneIdMap, training.getUser()).isAfter(training.getTimeEnd()))
                .filter(training -> dateTimeNow.toLocalTime().isAfter(training.getTimeEnd()))
                .forEach(training -> {
                    User user = training.getUser();
                    TrainSubscription subscription = getUserSubscription(userTrainSubscriptionMap, user);

                    TrainingEvent trainingEvent = trainingEventService.save(new TrainingEvent()
                            .setTraining(training)
                            .setTrainSubscription(subscription)
                            .setUser(user)
                            .setDateTime(dateTimeNow));

                    String responseText = "<b>Прошла тренировка: </b>\n" +
                            DateUtils.formatShortTime(training.getTimeStart()) + " — " + training.getName() + "\n";
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(user.getUserId());
                    sendMessage.enableHtml(true);
                    sendMessage.setReplyMarkup(getCancelTrainingKeyboard(trainingEvent.getId()));
                    sendMessage.setText(responseText);

                    try {
                        bot.execute(sendMessage);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    } finally {
                        reduceSubscriptionCountLeft(subscription);
                    }
                });
    }

    private InlineKeyboardMarkup getCancelTrainingKeyboard(Long eventId) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> cancelTrainingRow = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(Emoji.CANCELLATION.getEmoji() + "Отменить");
        cancelButton.setCallbackData(COMMAND_NAME + "_c" + eventId);
        cancelTrainingRow.add(cancelButton);

        rows.add(cancelTrainingRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private TrainSubscription getUserSubscription(Map<User, TrainSubscription> userTrainSubscriptionMap, User user) {
        TrainSubscription trainSubscription = userTrainSubscriptionMap.get(user);

        if (trainSubscription == null) {
            trainSubscription = trainSubscriptionService.getFirstActive(user);
            userTrainSubscriptionMap.put(user, trainSubscription);
        }

        return trainSubscription;
    }

    private LocalTime getUserTime(Map<User, ZoneId> userZoneIdMap, User user) {
        ZoneId zoneId = userZoneIdMap.get(user);

        if (zoneId == null) {
            zoneId = userCityService.getZoneIdOfUser(new Chat().setChatId(user.getUserId()), user);
            if (zoneId == null) {
                zoneId = ZoneId.systemDefault();
            }

            userZoneIdMap.put(user, zoneId);
        }

        return ZonedDateTime.now(zoneId).toLocalTime();
    }

    private void reduceSubscriptionCountLeft(TrainSubscription trainSubscription) {
        if (trainSubscription == null) {
            return;
        }

        trainSubscription.setCountLeft(trainSubscription.getCountLeft() - 1);
        if (trainSubscription.getCountLeft() <= 0) {
            trainSubscription.setActive(false);
        }

        trainSubscriptionService.save(trainSubscription);
    }
    
}
