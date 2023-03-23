package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.TrainSubscription;
import org.telegram.bot.domain.entities.TrainingEvent;
import org.telegram.bot.domain.entities.TrainingScheduled;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.DateUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.formatDate;
import static org.telegram.bot.utils.DateUtils.formatShortTime;
import static org.telegram.bot.utils.TextUtils.BORDER;

@Component
@RequiredArgsConstructor
public class Training implements CommandParent<PartialBotApiMethod<?>> {

    private final TrainingScheduledService trainingScheduledService;
    private final TrainSubscriptionService trainSubscriptionService;
    private final TrainingEventService trainingEventService;
    private final TrainingService trainingService;
    private final SpeechService speechService;

    private static final String COMMAND_NAME = "training";

    @Override
    public PartialBotApiMethod<?> parse(Update update) {
        final String addCommand = "_add";
        final String cancelCommand = "_c";

        Message message = getMessageFromUpdate(update);
        Long userId;
        Integer editMessageId = null;
        String textMessage;
        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();

            textMessage = cutCommandInText(callbackQuery.getData());
            userId = callbackQuery.getFrom().getId();
            editMessageId = message.getMessageId();
        } else {
            textMessage = cutCommandInText(message.getText());
            userId = message.getFrom().getId();
        }
        User user = new User().setUserId(userId);

        InlineKeyboardMarkup inlineKeyboardMarkup = null;
        String responseText;
        if (textMessage != null) {
            if (textMessage.startsWith(addCommand)) {
                Long trainingId = null;
                try {
                    trainingId = Long.parseLong(textMessage.substring(addCommand.length()));
                } catch (NumberFormatException ignored) {
                }

                LocalDateTime dateTimeNow = LocalDateTime.now();
                if (trainingId != null && update.hasCallbackQuery()) {
                    org.telegram.bot.domain.entities.Training training = trainingService.get(user, trainingId);
                    if (training.getTimeStart().isAfter(dateTimeNow.toLocalTime())) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }

                    TrainSubscription subscription = trainSubscriptionService.getFirstActive(user);
                    if (subscription != null) {
                        subscription.setCountLeft(subscription.getCountLeft() - 1);
                        trainSubscriptionService.save(subscription);
                    }

                    trainingEventService.save(new TrainingEvent()
                            .setUser(user)
                            .setTraining(training)
                            .setTrainSubscription(subscription)
                            .setDateTime(dateTimeNow)
                            .setCanceled(false)
                            .setUnplanned(true));

                    responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED) + "\n" + BORDER + getMainMenuText(user);
                    inlineKeyboardMarkup = getMainKeyboard();
                } else {
                    responseText = "<b>Выбери тренировку:</b>";
                    List<org.telegram.bot.domain.entities.Training> trainingList = trainingService.get(user);
                    if (trainingList.isEmpty()) {
                        responseText = responseText + "сначала заполни номенклатуру с помощью команды /set";
                        inlineKeyboardMarkup = getMainKeyboard();
                    } else {
                        inlineKeyboardMarkup = getKeyboardWithTrainingList(trainingList);
                    }
                }
            } else if (textMessage.startsWith(cancelCommand)) {
                long eventId;
                try {
                    eventId = Long.parseLong(textMessage.substring(cancelCommand.length()));
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
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        } else {
            responseText = getMainMenuText(user);
            inlineKeyboardMarkup = getMainKeyboard();
        }

        if (editMessageId != null) {
            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setChatId(message.getChatId().toString());
            editMessageText.setMessageId(message.getMessageId());
            editMessageText.enableHtml(true);
            editMessageText.setText(responseText);
            editMessageText.setReplyMarkup(inlineKeyboardMarkup);

            return editMessageText;
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String getMainMenuText(User user) {
        LocalDate dateNow = LocalDate.now();

        DayOfWeek dayOfWeekToday = DayOfWeek.from(dateNow);
        DayOfWeek dayOfWeekTomorrow = DayOfWeek.from(dateNow.plusDays(1));

        List<TrainingScheduled> trainingScheduledList = trainingScheduledService.get(user);
        List<org.telegram.bot.domain.entities.Training> unplannedTodayTrainings = trainingEventService.getAllUnplanned(user, dateNow)
                .stream()
                .map(TrainingEvent::getTraining)
                .collect(Collectors.toList());

        StringBuilder buf = new StringBuilder();
        buf.append("<b>Тренировки сегодня:</b>\n");
        buf.append(buildFormattedTrainingList(trainingScheduledList, dayOfWeekToday));
        buf.append(buildFormattedTrainingList(unplannedTodayTrainings));
        buf.append("<b>Тренировки завтра:</b>\n");
        buf.append(buildFormattedTrainingList(trainingScheduledList, dayOfWeekTomorrow));

        buf.append(BORDER);

        TrainSubscription subscription = trainSubscriptionService.getFirstActive(user);
        buf.append("<b>Текущий абонемент:</b>");
        if (subscription != null) {
            LocalDate expirationDate = subscription.getStartDate().plus(subscription.getPeriod());
            LocalDate actualExpirationDate = calculateActualExpirationDate(subscription, trainingScheduledList);

            buf.append(" (").append(subscription.getCount()).append(")\n")
                    .append("Годен до: <b>").append(formatDate(expirationDate)).append("</b>\n")
                    .append("Осталось занятий: <b>").append(subscription.getCountLeft()).append("</b>\n");

            if (actualExpirationDate != null) {
                buf.append("Хватит до: <b>").append(formatDate(actualExpirationDate)).append("</b>");
            }

        } else {
            buf.append("\nотсутствует");
        }

        return buf.toString();
    }

    private String buildFormattedTrainingList(List<TrainingScheduled> trainingScheduledList, DayOfWeek dayOfWeek) {
        List<org.telegram.bot.domain.entities.Training> trainingList = filterScheduledListByDayOfWeek(trainingScheduledList, dayOfWeek)
                .stream()
                .map(TrainingScheduled::getTraining)
                .collect(Collectors.toList());

        return buildFormattedTrainingList(trainingList);
    }

    private String buildFormattedTrainingList(List<org.telegram.bot.domain.entities.Training> trainingList) {
        StringBuilder buf = new StringBuilder();

        trainingList
                .stream()
                .filter(training -> !training.getDeleted())
                .sorted(Comparator.comparing(org.telegram.bot.domain.entities.Training::getTimeStart))
                .forEach(training -> buf.append(DateUtils.formatShortTime(training.getTimeStart())).append(" — ")
                        .append(training.getName()).append("\n"));

        return buf.toString();
    }

    private LocalDate calculateActualExpirationDate(TrainSubscription subscription, List<TrainingScheduled> scheduledList) {
        if (scheduledList.isEmpty()) {
            return null;
        }

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

    private InlineKeyboardMarkup getKeyboardWithTrainingList(List<org.telegram.bot.domain.entities.Training> trainingList) {
        LocalTime timeNow = LocalTime.now();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        trainingList
                .stream()
                .filter(training -> training.getTimeStart().isBefore(timeNow))
                .forEach(training -> {
                    List<InlineKeyboardButton> buttonRow = new ArrayList<>();
                    InlineKeyboardButton trainingButton = new InlineKeyboardButton();
                    trainingButton.setText(Emoji.NEW.getEmoji() + training.getName() + " " + formatShortTime(training.getTimeStart()));
                    trainingButton.setCallbackData(COMMAND_NAME + "_add" + training.getId());
                    buttonRow.add(trainingButton);

                    rows.add(buttonRow);
        });

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup getMainKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> addTrainingRow = new ArrayList<>();
        InlineKeyboardButton trainingButton = new InlineKeyboardButton();
        trainingButton.setText(Emoji.NEW.getEmoji() + "Незапл. тренировка");
        trainingButton.setCallbackData(COMMAND_NAME + "_add");
        addTrainingRow.add(trainingButton);

        List<InlineKeyboardButton> settingsRow = new ArrayList<>();
        InlineKeyboardButton settingsButton = new InlineKeyboardButton();
        settingsButton.setText(Emoji.GEAR.getEmoji() + "Установки");
        settingsButton.setCallbackData("установить тренировки");
        settingsRow.add(settingsButton);

        rows.add(addTrainingRow);
        rows.add(settingsRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }
}
