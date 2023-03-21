package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.TrainSubscription;
import org.telegram.bot.domain.entities.TrainingEvent;
import org.telegram.bot.domain.entities.TrainingScheduled;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.TrainSubscriptionService;
import org.telegram.bot.services.TrainingEventService;
import org.telegram.bot.services.TrainingScheduledService;
import org.telegram.bot.utils.DateUtils;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.formatDate;

@Component
@RequiredArgsConstructor
public class Training implements CommandParent<SendMessage> {

    private final TrainingScheduledService trainingScheduledService;
    private final TrainSubscriptionService trainSubscriptionService;
    private final TrainingEventService trainingEventService;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());

        User user = new User().setUserId(message.getFrom().getId());
        LocalDateTime dateTimeNow = LocalDateTime.now();
        String responseText;
        if (textMessage != null) {
            if (!textMessage.startsWith("_")) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            long eventId;
            try {
                eventId = Long.parseLong(textMessage.substring(textMessage.indexOf("_") + 2));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            TrainingEvent trainingEvent = trainingEventService.get(user, eventId);
            if (trainingEvent == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            TrainSubscription subscription = trainSubscriptionService.getFirstActive(user);
            if (subscription != null) {
                subscription.setCountLeft(subscription.getCountLeft() + trainingEvent.getTraining().getCost());
                trainSubscriptionService.save(subscription);
            }

            trainingEventService.save(trainingEvent.setCanceled(true));

            responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
        } else {
            DayOfWeek dayOfWeekToday = DayOfWeek.from(dateTimeNow);
            DayOfWeek dayOfWeekTomorrow = DayOfWeek.from(dateTimeNow.plusDays(1));

            List<TrainingScheduled> trainingScheduledList = trainingScheduledService.get(user);

            StringBuilder buf = new StringBuilder();
            buf.append("<b>Тренировки сегодня:</b>\n");
            buf.append(buildFormattedTrainingList(trainingScheduledList, dayOfWeekToday));
            buf.append("<b>Тренировки завтра:</b>\n");
            buf.append(buildFormattedTrainingList(trainingScheduledList, dayOfWeekTomorrow));

            buf.append(TextUtils.BORDER);

            TrainSubscription subscription = trainSubscriptionService.getFirstActive(user);
            buf.append("<b>Текущий абонемент:</b>");
            if (subscription != null) {
                LocalDate expirationDate = subscription.getStartDate().plus(subscription.getPeriod());
                LocalDate actualExpirationDate = calculateActualExpirationDate(subscription, trainingScheduledList);

                buf.append(" (").append(subscription.getCount()).append(")\n")
                        .append("Годен до: <b>").append(formatDate(expirationDate)).append("</b>\n")
                        .append("Осталось занятий: <b>").append(subscription.getCountLeft()).append("</b>\n")
                        .append("Хватит до: <b>").append(formatDate(actualExpirationDate)).append("</b>");
            } else {
                buf.append("\nотсутствует");
            }

            responseText = buf.toString();
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String buildFormattedTrainingList(List<TrainingScheduled> trainingScheduledList, DayOfWeek dayOfWeek) {
        return buildFormattedTrainingList(filterScheduledListByDayOfWeek(trainingScheduledList, dayOfWeek));
    }

    private String buildFormattedTrainingList(List<TrainingScheduled> trainingScheduledList) {
        StringBuilder buf = new StringBuilder();

        trainingScheduledList
                .stream()
                .map(TrainingScheduled::getTraining)
                .filter(training -> !training.getDeleted())
                .sorted(Comparator.comparing(org.telegram.bot.domain.entities.Training::getTime))
                .forEach(training -> buf.append(DateUtils.formatShortTime(training.getTime())).append(" — ")
                        .append(training.getName()).append("\n"));

        return buf.toString();
    }

    private LocalDate calculateActualExpirationDate(TrainSubscription subscription, List<TrainingScheduled> scheduledList) {
        LocalDate expirationDate = LocalDate.now();
        float countLeft = subscription.getCountLeft();

        while (countLeft >= 0) {
            DayOfWeek dayOfWeek = expirationDate.getDayOfWeek();
            float sumCostOfDay = filterScheduledListByDayOfWeek(scheduledList, dayOfWeek)
                    .stream()
                    .map(TrainingScheduled::getTraining)
                    .map(org.telegram.bot.domain.entities.Training::getCost)
                    .reduce(Float::sum)
                    .orElse(0F);

            if (sumCostOfDay >= countLeft) {
                return expirationDate;
            }

            countLeft = countLeft - sumCostOfDay;
            expirationDate = expirationDate.plusDays(1);
        }


        return expirationDate;
    }

    private List<TrainingScheduled> filterScheduledListByDayOfWeek(List<TrainingScheduled> trainingScheduledList, DayOfWeek dayOfWeek) {
        return trainingScheduledList
                .stream()
                .filter(trainingScheduled -> dayOfWeek.equals(trainingScheduled.getDayOfWeek()))
                .collect(Collectors.toList());
    }
}
