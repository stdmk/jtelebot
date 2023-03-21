package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.services.TrainSubscriptionService;
import org.telegram.bot.services.TrainingEventService;
import org.telegram.bot.services.TrainingScheduledService;
import org.telegram.bot.utils.DateUtils;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
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

    @Override
    @Scheduled(fixedRate = 600000)
    public void execute() {
        LocalDateTime dateTimeNow = LocalDateTime.now();
        DayOfWeek currentDayOfWeek = DayOfWeek.from(dateTimeNow);

        Map<User, TrainSubscription> userTrainSubscriptionMap = new HashMap<>();
        List<Long> pastTrainingIdList = trainingEventService.getAll(dateTimeNow.toLocalDate())
                .stream()
                .map(TrainingEvent::getTraining)
                .map(Training::getId)
                .collect(Collectors.toList());

        trainingScheduledService.getAll(currentDayOfWeek)
                .stream()
                .map(TrainingScheduled::getTraining)
                .filter(training -> !pastTrainingIdList.contains(training.getId()))
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
                            DateUtils.formatShortTime(training.getTimeStart()) + " — " + training.getName() + "\n" +
                            "/training_c" + trainingEvent.getId() + " — отменить \n" +
                            TextUtils.BORDER +
                            "/training  — общая информация";
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(user.getUserId());
                    sendMessage.enableHtml(true);
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

    private TrainSubscription getUserSubscription(Map<User, TrainSubscription> userTrainSubscriptionMap, User user) {
        TrainSubscription trainSubscription = userTrainSubscriptionMap.get(user);

        if (trainSubscription == null) {
            trainSubscription = trainSubscriptionService.getFirstActive(user);
            userTrainSubscriptionMap.put(user, trainSubscription);
        }

        return trainSubscription;
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
