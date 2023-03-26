package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.telegram.bot.utils.DateUtils.*;
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
    private static final String ADD_COMMAND = "_add";
    private static final String CANCEL_COMMAND = "_c";
    private static final String REPORT_COMMAND = "_r";
    private static final String REPORT_SUBSCRIPTION_COMMAND = REPORT_COMMAND + "_sub";
    private static final String CALLBACK_REPORT_SUBSCRIPTION_COMMAND = COMMAND_NAME + REPORT_SUBSCRIPTION_COMMAND;
    private static final String REPORT_MONTH_COMMAND = REPORT_COMMAND + "m";
    private static final String CALLBACK_REPORT_MONTH_COMMAND = COMMAND_NAME + REPORT_MONTH_COMMAND;
    private static final String REPORT_YEAR_COMMAND = REPORT_COMMAND + "y";
    private static final String CALLBACK_REPORT_YEAR_COMMAND = COMMAND_NAME + REPORT_YEAR_COMMAND;
    private static final String REPORT_ALL_TIME_COMMAND = REPORT_COMMAND + "all";
    private static final String CALLBACK_REPORT_ALL_COMMAND = COMMAND_NAME + REPORT_ALL_TIME_COMMAND;

    @Override
    public PartialBotApiMethod<?> parse(Update update) {
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
            if (textMessage.startsWith(ADD_COMMAND)) {
                Long trainingId = null;
                try {
                    trainingId = Long.parseLong(textMessage.substring(ADD_COMMAND.length()));
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
            } else if (textMessage.startsWith(CANCEL_COMMAND)) {
                long eventId;
                try {
                    eventId = Long.parseLong(textMessage.substring(CANCEL_COMMAND.length()));
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
            } else if (textMessage.startsWith(REPORT_COMMAND)) {
                int page = 0;
                LocalDate dateNow = LocalDate.now();

                if (textMessage.startsWith(REPORT_ALL_TIME_COMMAND)) {
                    responseText = getReportStatistic(trainingEventService.getAll(user));
                } else if (textMessage.startsWith(REPORT_YEAR_COMMAND)) {
                    responseText = getReportStatistic(trainingEventService.getAllOfYear(user, dateNow.getYear()));
                } else if (textMessage.startsWith(REPORT_MONTH_COMMAND)) {
                    responseText = getReportStatistic(trainingEventService.getAllOfMonth(user, dateNow.getMonthValue()));
                } else if (textMessage.startsWith(REPORT_SUBSCRIPTION_COMMAND)) {
                    long subscriptionId;
                    try {
                        subscriptionId = Long.parseLong(textMessage.substring(REPORT_SUBSCRIPTION_COMMAND.length()));
                    } catch (NumberFormatException e) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                    }

                    TrainSubscription subscription = trainSubscriptionService.get(subscriptionId, user);
                    if (subscription == null) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                    }

                    responseText = getReportStatistic(trainingEventService.getAll(user, subscription));
                } else {
                    try {
                        page = Integer.parseInt(textMessage.substring(REPORT_COMMAND.length()));
                    } catch (NumberFormatException ignored) {
                    }

                    responseText = "<b>Выбери отчёт:</b>\n";
                }

                inlineKeyboardMarkup = getReportKeyboard(user, page);
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

    private String getReportStatistic(List<TrainingEvent> trainingEventList) {
        if (trainingEventList.isEmpty()) {
            return "Статистика отсутствует";
        }

        List<TrainingEvent> nonCaceledTrainingEventList = trainingEventList
                .stream()
                .filter(trainingEvent -> !Boolean.TRUE.equals(trainingEvent.getCanceled()))
                .collect(Collectors.toList());

        StringBuilder buf = new StringBuilder();

        LocalDateTime firstEventDateTime = nonCaceledTrainingEventList
                .stream()
                .min(Comparator.comparing(TrainingEvent::getDateTime))
                .orElseThrow(() -> new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)))
                .getDateTime();
        buf.append("Начиная с <b>").append(formatDate(firstEventDateTime)).append("</b>\n");

        buf.append("Тренировок: ").append(" <b>").append(nonCaceledTrainingEventList.size()).append("</b>\n");

        long canceledCount = trainingEventList
                .stream()
                .filter(trainingEvent -> Boolean.TRUE.equals(trainingEvent.getCanceled()))
                .count();
        buf.append("Отменённых: <b>").append(canceledCount).append("</b>\n");

        long unplannedCount = nonCaceledTrainingEventList
                .stream()
                .filter(trainingEvent -> Boolean.TRUE.equals(trainingEvent.getUnplanned()))
                .count();
        buf.append("Незапланированных: <b>").append(unplannedCount).append("</b>\n");

        buf.append(BORDER);

        nonCaceledTrainingEventList
                .stream()
                .map(trainingEvent -> trainingEvent.getTraining().getName())
                .distinct()
                .forEach(name -> {
                    long trainingByNameCount = nonCaceledTrainingEventList
                            .stream()
                            .filter(trainingEvent -> name.equals(trainingEvent.getTraining().getName()))
                            .count();
                    buf.append(name).append(": <b>").append(trainingByNameCount).append("</b>\n");
                });

        buf.append(BORDER);

        Duration allTrainingsDuration = nonCaceledTrainingEventList
                .stream()
                .map(trainingEvent -> getDuration(trainingEvent.getTraining().getTimeStart(), trainingEvent.getTraining().getTimeEnd()))
                .reduce(Duration::plus)
                .orElse(Duration.ZERO);
        buf.append("Всего время: <b>").append(DateUtils.durationToString(allTrainingsDuration)).append("</b>\n");

        Float sumOfCost = nonCaceledTrainingEventList
                .stream()
                .map(trainingEvent -> trainingEvent.getTraining().getCost())
                .reduce(Float::sum)
                .orElse(0F);
        buf.append("Всего стоимость: <b>").append(sumOfCost).append("</b>\n");

        return buf.toString();
    }

    private String getMainMenuText(User user) {
        LocalDate dateNow = LocalDate.now();

        DayOfWeek dayOfWeekToday = DayOfWeek.from(dateNow);

        List<TrainingScheduled> trainingScheduledList = trainingScheduledService.get(user);
        List<org.telegram.bot.domain.entities.Training> unplannedTodayTrainings = trainingEventService.getAllUnplanned(user, dateNow)
                .stream()
                .map(TrainingEvent::getTraining)
                .collect(Collectors.toList());
        List<org.telegram.bot.domain.entities.Training> canceledTodayTrainings = trainingEventService.getAllCanceled(user, dateNow)
                .stream()
                .map(TrainingEvent::getTraining)
                .collect(Collectors.toList());

        StringBuilder buf = new StringBuilder();
        buf.append("<b>Тренировки сегодня:</b>\n");
        buf.append(buildFormattedTrainingList(
                getTrainingsByDayOfWeek(trainingScheduledList, unplannedTodayTrainings, canceledTodayTrainings, dayOfWeekToday)));
        buf.append("<b>Тренировки завтра:</b>\n");
        buf.append(buildFormattedTrainingList(getTrainingsByDayOfWeek(trainingScheduledList, dayOfWeekToday.plus(1))));

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

    private List<org.telegram.bot.domain.entities.Training> getTrainingsByDayOfWeek(List<TrainingScheduled> trainingScheduledList, DayOfWeek dayOfWeek) {
        return getTrainingsByDayOfWeek(trainingScheduledList, new ArrayList<>(), new ArrayList<>(), dayOfWeek);
    }

    private List<org.telegram.bot.domain.entities.Training> getTrainingsByDayOfWeek(List<TrainingScheduled> trainingScheduledList,
                                                                                    List<org.telegram.bot.domain.entities.Training> unplannedTrainingList,
                                                                                    List<org.telegram.bot.domain.entities.Training> canceledTrainingList,
                                                                                    DayOfWeek dayOfWeek) {
        List<Long> canceledTrainingIdList = canceledTrainingList.stream().map(org.telegram.bot.domain.entities.Training::getId).collect(Collectors.toList());
        Stream<org.telegram.bot.domain.entities.Training> plannedTrainingsTodayStream =
                getScheduledTrainingsByWeekDay(trainingScheduledList, dayOfWeek);

        return Stream.concat(plannedTrainingsTodayStream, unplannedTrainingList.stream())
                .filter(training -> !canceledTrainingIdList.contains(training.getId()))
                .sorted(Comparator.comparing(org.telegram.bot.domain.entities.Training::getTimeStart))
                .collect(Collectors.toList());
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
            float sumCostOfDay = getScheduledTrainingsByWeekDay(scheduledList, dayOfWeek)
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

    private Stream<org.telegram.bot.domain.entities.Training> getScheduledTrainingsByWeekDay(List<TrainingScheduled> trainingScheduledList, DayOfWeek dayOfWeek) {
        return trainingScheduledList
                .stream()
                .filter(trainingScheduled -> dayOfWeek.equals(trainingScheduled.getDayOfWeek()))
                .map(TrainingScheduled::getTraining);
    }

    private InlineKeyboardMarkup getKeyboardWithTrainingList(List<org.telegram.bot.domain.entities.Training> trainingList) {
        LocalTime timeNow = LocalTime.now();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        trainingList
                .stream()
                .filter(training -> training.getTimeStart().isBefore(timeNow))
                .sorted(Comparator.comparing(org.telegram.bot.domain.entities.Training::getTimeStart))
                .forEach(training -> {
                    List<InlineKeyboardButton> buttonRow = new ArrayList<>();
                    InlineKeyboardButton trainingButton = new InlineKeyboardButton();
                    trainingButton.setText(Emoji.NEW.getEmoji() + training.getName() + " " + formatShortTime(training.getTimeStart()));
                    trainingButton.setCallbackData(COMMAND_NAME + ADD_COMMAND + training.getId());
                    buttonRow.add(trainingButton);

                    rows.add(buttonRow);
        });

        rows.add(getTrainingMainInfoButtonRow());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup getReportKeyboard(User user, int page) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        Page<TrainSubscription> trainSubscriptionPage = trainSubscriptionService.get(user, page);

        trainSubscriptionPage.forEach(subscription -> {
            String buttonCaption = formatDate(subscription.getStartDate()) + " — " +
                    formatDate(subscription.getStartDate().plus(subscription.getPeriod())) +
                    " (" + subscription.getCount() + ")\n";

            List<InlineKeyboardButton> reportForSubscriptionRow = new ArrayList<>();
            InlineKeyboardButton subscriptionReportButton = new InlineKeyboardButton();
            subscriptionReportButton.setText(buttonCaption);
            subscriptionReportButton.setCallbackData(CALLBACK_REPORT_SUBSCRIPTION_COMMAND + subscription.getId());
            reportForSubscriptionRow.add(subscriptionReportButton);

            rows.add(reportForSubscriptionRow);
        });

        List<InlineKeyboardButton> pagesRow = new ArrayList<>();
        if (page > 0) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText(Emoji.LEFT_ARROW.getEmoji() + "Назад");
            backButton.setCallbackData(COMMAND_NAME + REPORT_COMMAND + (page - 1));

            pagesRow.add(backButton);
        }

        if (page + 1 < trainSubscriptionPage.getTotalPages()) {
            InlineKeyboardButton forwardButton = new InlineKeyboardButton();
            forwardButton.setText("Вперёд" + Emoji.RIGHT_ARROW.getEmoji());
            forwardButton.setCallbackData(COMMAND_NAME + REPORT_COMMAND + (page + 1));

            pagesRow.add(forwardButton);
        }

        rows.add(pagesRow);

        List<InlineKeyboardButton> reportForMonthRow = new ArrayList<>();
        InlineKeyboardButton monthReportButton = new InlineKeyboardButton();
        monthReportButton.setText("За текущий месяц");
        monthReportButton.setCallbackData(CALLBACK_REPORT_MONTH_COMMAND);
        reportForMonthRow.add(monthReportButton);

        List<InlineKeyboardButton> reportForYearRow = new ArrayList<>();
        InlineKeyboardButton yearReportButton = new InlineKeyboardButton();
        yearReportButton.setText("За текущий год");
        yearReportButton.setCallbackData(CALLBACK_REPORT_YEAR_COMMAND);
        reportForYearRow.add(yearReportButton);

        List<InlineKeyboardButton> reportForAllTimeRow = new ArrayList<>();
        InlineKeyboardButton allTimeReportButton = new InlineKeyboardButton();
        allTimeReportButton.setText("За всё время");
        allTimeReportButton.setCallbackData(CALLBACK_REPORT_ALL_COMMAND);
        reportForAllTimeRow.add(allTimeReportButton);

        rows.add(reportForMonthRow);
        rows.add(reportForYearRow);
        rows.add(reportForAllTimeRow);
        rows.add(getTrainingMainInfoButtonRow());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }
    
    private List<InlineKeyboardButton> getTrainingMainInfoButtonRow() {
        List<InlineKeyboardButton> infoButtonRow = new ArrayList<>();
        InlineKeyboardButton infoButton = new InlineKeyboardButton();
        infoButton.setText(Emoji.WEIGHT_LIFTER.getEmoji() + "Тренировки");
        infoButton.setCallbackData(COMMAND_NAME);
        infoButtonRow.add(infoButton);
        
        return infoButtonRow;
    }

    private InlineKeyboardMarkup getMainKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> addTrainingRow = new ArrayList<>();
        InlineKeyboardButton trainingButton = new InlineKeyboardButton();
        trainingButton.setText(Emoji.NEW.getEmoji() + "Незапл. тренировка");
        trainingButton.setCallbackData(COMMAND_NAME + ADD_COMMAND);
        addTrainingRow.add(trainingButton);

        List<InlineKeyboardButton> reportTrainingRow = new ArrayList<>();
        InlineKeyboardButton reportButton = new InlineKeyboardButton();
        reportButton.setText(Emoji.MEMO.getEmoji() + "Отчёты");
        reportButton.setCallbackData(COMMAND_NAME + REPORT_COMMAND);
        reportTrainingRow.add(reportButton);

        List<InlineKeyboardButton> settingsRow = new ArrayList<>();
        InlineKeyboardButton settingsButton = new InlineKeyboardButton();
        settingsButton.setText(Emoji.GEAR.getEmoji() + "Установки");
        settingsButton.setCallbackData("установить тренировки");
        settingsRow.add(settingsButton);

        rows.add(addTrainingRow);
        rows.add(reportTrainingRow);
        rows.add(settingsRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }
}
