package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.DateUtils;
import org.telegram.bot.utils.TextUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.telegram.bot.utils.DateUtils.*;
import static org.telegram.bot.utils.MathUtils.getPercentValue;
import static org.telegram.bot.utils.TextUtils.BORDER;
import static org.telegram.bot.utils.TextUtils.cutHtmlTags;

@Component
@RequiredArgsConstructor
public class Training implements Command {

    private final Bot bot;
    private final TrainingScheduledService trainingScheduledService;
    private final TrainSubscriptionService trainSubscriptionService;
    private final TrainingEventService trainingEventService;
    private final TrainingService trainingService;
    private final TrainingStoppedService trainingStoppedService;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final LanguageResolver languageResolver;
    private final InternationalizationService internationalizationService;

    private static final String COMMAND_NAME = "training";
    private static final String ADD_COMMAND = "_add";
    private static final String CANCEL_COMMAND = "_c";
    private static final String CANCEL_REASON_COMMAND = "_cr";
    private static final String REPORT_COMMAND = "_r";
    private static final String DOWNLOAD_COMMAND = "_d";

    private static final String REPORT_SUBSCRIPTION_COMMAND = REPORT_COMMAND + "_sub";
    private static final String CALLBACK_REPORT_SUBSCRIPTION_COMMAND = COMMAND_NAME + REPORT_SUBSCRIPTION_COMMAND;
    private static final String REPORT_MONTH_COMMAND = REPORT_COMMAND + "m";
    private static final String CALLBACK_REPORT_MONTH_COMMAND = COMMAND_NAME + REPORT_MONTH_COMMAND;
    private static final String REPORT_YEAR_COMMAND = REPORT_COMMAND + "y";
    private static final String CALLBACK_REPORT_YEAR_COMMAND = COMMAND_NAME + REPORT_YEAR_COMMAND;
    private static final String REPORT_ALL_TIME_COMMAND = REPORT_COMMAND + "all";
    private static final String CALLBACK_REPORT_ALL_COMMAND = COMMAND_NAME + REPORT_ALL_TIME_COMMAND;
    private static final String DOWNLOAD_REPORT_SUBSCRIPTION_COMMAND = DOWNLOAD_COMMAND + "_sub";
    private static final String CALLBACK_DOWNLOAD_REPORT_SUBSCRIPTION_COMMAND = COMMAND_NAME + DOWNLOAD_REPORT_SUBSCRIPTION_COMMAND;
    private static final String DOWNLOAD_REPORT_MONTH_COMMAND = DOWNLOAD_COMMAND + "m";
    private static final String CALLBACK_DOWNLOAD_REPORT_MONTH_COMMAND = COMMAND_NAME + DOWNLOAD_REPORT_MONTH_COMMAND;
    private static final String DOWNLOAD_REPORT_YEAR_COMMAND = DOWNLOAD_COMMAND + "y";
    private static final String CALLBACK_DOWNLOAD_REPORT_YEAR_COMMAND = COMMAND_NAME + DOWNLOAD_REPORT_YEAR_COMMAND;
    private static final String DOWNLOAD_REPORT_ALL_TIME_COMMAND = DOWNLOAD_COMMAND + "all";
    private static final String CALLBACK_DOWNLOAD_REPORT_ALL_COMMAND = COMMAND_NAME + DOWNLOAD_REPORT_ALL_TIME_COMMAND;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        User user = message.getUser();
        Chat chat = message.getChat();

        String commandArgument;
        CommandWaiting commandWaiting = commandWaitingService.get(chat, user);
        if (!message.isCallback() && commandWaiting != null) {
            commandArgument = TextUtils.cutCommandInText(commandWaiting.getTextMessage() + message.getText());
        } else {
            commandArgument = message.getCommandArgument();
        }

        Keyboard keyboard = null;
        String responseText;
        if (commandArgument != null) {
            if (commandArgument.startsWith(ADD_COMMAND)) {
                Long trainingId = null;
                try {
                    trainingId = Long.parseLong(commandArgument.substring(ADD_COMMAND.length()));
                } catch (NumberFormatException ignored) {
                    // not training id
                }

                LocalDateTime dateTimeNow = LocalDateTime.now();
                if (trainingId != null && message.isCallback()) {
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
                    keyboard = getMainKeyboard();
                } else {
                    responseText = "<b>${command.training.choosetraining}:</b>";
                    List<org.telegram.bot.domain.entities.Training> trainingList = trainingService.get(user);
                    if (trainingList.isEmpty()) {
                        responseText = responseText + "${command.training.emptynomenclature} /set";
                        keyboard = getMainKeyboard();
                    } else {
                        keyboard = getKeyboardWithTrainingList(trainingList);
                    }
                }
            } else if (commandArgument.startsWith(CANCEL_COMMAND)) {
                commandWaitingService.remove(chat, user);

                String cancellationReason = null;
                String eventIdString;
                if (commandArgument.contains(CANCEL_REASON_COMMAND)) {
                    int cancellationReasonCommandIndex = commandArgument.indexOf(CANCEL_REASON_COMMAND);
                    eventIdString = commandArgument.substring(CANCEL_COMMAND.length(), cancellationReasonCommandIndex);
                    cancellationReason = commandArgument.substring(cancellationReasonCommandIndex + 4);
                } else {
                    eventIdString = commandArgument.substring(CANCEL_COMMAND.length());
                }

                long eventId;
                try {
                    eventId = Long.parseLong(eventIdString);
                } catch (NumberFormatException e) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                if (cancellationReason != null) {
                    TrainingEvent trainingEvent = trainingEventService.get(user, eventId);
                    if (trainingEvent == null) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }

                    TrainSubscription subscription = trainSubscriptionService.getFirstActive(user);
                    if (subscription != null) {
                        subscription.setCountLeft(subscription.getCountLeft() + trainingEvent.getTraining().getCost());
                        trainSubscriptionService.save(subscription);
                    }

                    trainingEvent.setCanceled(true);
                    trainingEvent.setCancellationReason(cancellationReason);

                    trainingEventService.save(trainingEvent);

                    responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
                } else {
                    commandWaitingService.add(chat, user, Training.class, COMMAND_NAME + CANCEL_COMMAND + eventId + CANCEL_REASON_COMMAND);
                    responseText = "${command.training.rejectionreason}";
                }
            } else if (commandArgument.startsWith(REPORT_COMMAND)) {
                int page = 0;
                LocalDate dateNow = LocalDate.now();
                String reportDownloadCommand = null;

                if (commandArgument.startsWith(REPORT_ALL_TIME_COMMAND)) {
                    responseText = getReportStatistic(trainingEventService.getAll(user));
                    reportDownloadCommand = CALLBACK_DOWNLOAD_REPORT_ALL_COMMAND;
                } else if (commandArgument.startsWith(REPORT_YEAR_COMMAND)) {
                    responseText = getReportStatistic(trainingEventService.getAllOfYear(user, dateNow.getYear()));
                    reportDownloadCommand = CALLBACK_DOWNLOAD_REPORT_YEAR_COMMAND;
                } else if (commandArgument.startsWith(REPORT_MONTH_COMMAND)) {
                    responseText = getReportStatistic(trainingEventService.getAllOfMonth(user, dateNow.getMonthValue()));
                    reportDownloadCommand = CALLBACK_DOWNLOAD_REPORT_MONTH_COMMAND;
                } else if (commandArgument.startsWith(REPORT_SUBSCRIPTION_COMMAND)) {
                    long subscriptionId;
                    try {
                        subscriptionId = Long.parseLong(commandArgument.substring(REPORT_SUBSCRIPTION_COMMAND.length()));
                    } catch (NumberFormatException e) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                    }

                    TrainSubscription subscription = trainSubscriptionService.get(subscriptionId, user);
                    if (subscription == null) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                    }

                    responseText = getReportStatistic(trainingEventService.getAll(user, subscription));
                    reportDownloadCommand = CALLBACK_DOWNLOAD_REPORT_SUBSCRIPTION_COMMAND + subscriptionId;
                } else {
                    try {
                        page = Integer.parseInt(commandArgument.substring(REPORT_COMMAND.length()));
                    } catch (NumberFormatException ignored) {
                        // not page
                    }

                    responseText = "<b>${command.training.choosereport}:</b>\n";
                }

                keyboard = getReportKeyboard(user, page, reportDownloadCommand);
            } else if (commandArgument.startsWith(DOWNLOAD_COMMAND)) {
                LocalDate dateNow = LocalDate.now();
                String languageCode = languageResolver.getChatLanguageCode(message, user);
                File file;
                String caption;

                if (commandArgument.startsWith(DOWNLOAD_REPORT_ALL_TIME_COMMAND)) {
                    file = getReportFile(trainingEventService.getAll(user), "all", languageCode);
                    caption = "{command.training.alltimereport}";
                } else if (commandArgument.startsWith(DOWNLOAD_REPORT_YEAR_COMMAND)) {
                    int year = dateNow.getYear();
                    file = getReportFile(trainingEventService.getAllOfYear(user, year), String.valueOf(year), languageCode);
                    caption = "{command.training.yearreport} " + year;
                } else if (commandArgument.startsWith(DOWNLOAD_REPORT_MONTH_COMMAND)) {
                    String monthName = dateNow.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, languageResolver.getLocale(chat));
                    file = getReportFile(trainingEventService.getAllOfMonth(user, dateNow.getMonthValue()), monthName, languageCode);
                    caption = "{command.training.monthly} " + monthName;
                } else if (commandArgument.startsWith(DOWNLOAD_REPORT_SUBSCRIPTION_COMMAND)) {
                    long subscriptionId;
                    try {
                        subscriptionId = Long.parseLong(commandArgument.substring(DOWNLOAD_REPORT_SUBSCRIPTION_COMMAND.length()));
                    } catch (NumberFormatException e) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                    }

                    TrainSubscription subscription = trainSubscriptionService.get(subscriptionId, user);
                    if (subscription == null) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                    }

                    String subscriptionName = formatDate(subscription.getStartDate()) + " — " +
                            formatDate(subscription.getStartDate().plus(subscription.getPeriod())) +
                            " (" + subscription.getCount() + ")";
                    file = getReportFile(trainingEventService.getAll(user, subscription), subscriptionName, languageCode);
                    caption = "${command.training.subscriptionreport} " + subscriptionName;
                } else {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                }

                return returnResponse(new FileResponse(message)
                        .addFile(file)
                        .setText(caption));
            } else {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        } else {
            responseText = getMainMenuText(user);
            keyboard = getMainKeyboard();
        }

        if (message.isCallback()) {
            return returnResponse(new EditResponse(message)
                    .setText(responseText)
                    .setKeyboard(keyboard)
                    .setResponseSettings(new ResponseSettings()
                            .setFormattingStyle(FormattingStyle.HTML)));
        }

        return returnResponse(new TextResponse(message)                    
                .setText(responseText)
                .setKeyboard(keyboard)
                .setResponseSettings(new ResponseSettings()
                        .setFormattingStyle(FormattingStyle.HTML)));
    }

    private File getReportFile(List<TrainingEvent> trainingEventList, String fileName, String lang) {
        StringBuilder buf = new StringBuilder("Отчёт " + fileName + "\n\n");

        buf.append(cutHtmlTags(getReportStatistic(trainingEventList))).append(BORDER).append("\n");

        trainingEventList.forEach(trainingEvent -> {
            org.telegram.bot.domain.entities.Training training = trainingEvent.getTraining();
            TrainSubscription subscription = trainingEvent.getTrainSubscription();

            String subscriptionName;
            if (subscription == null) {
                subscriptionName = "";
            } else {
                subscriptionName = "${command.training.subscription}: " + formatDate(subscription.getStartDate()) + " — " +
                        formatDate(subscription.getStartDate().plus(subscription.getPeriod())) +
                        " (" + subscription.getCount() + ")\n";
            }
            String canceled = Boolean.TRUE.equals(trainingEvent.getCanceled()) ? "${command.training.canceled} (" + trainingEvent.getCancellationReason() + ")\n" : "";
            String unplanned = Boolean.TRUE.equals(trainingEvent.getUnplanned()) ? "${command.training.unplanned}\n" : "";

            buf.append(formatDate(trainingEvent.getDateTime())).append(" ").append(DateUtils.getDayOfWeek(trainingEvent.getDateTime(), lang)).append("\n")
                    .append(training.getName()).append(" (").append(training.getCost()).append(")").append("\n")
                    .append(canceled)
                    .append(unplanned)
                    .append(formatShortTime(training.getTimeStart())).append(" — ").append(formatShortTime(training.getTimeEnd()))
                    .append(" (").append(durationToString(training.getTimeEnd(), training.getTimeStart())).append(")\n")
                    .append(subscriptionName)
                    .append("\n");
        });

        String txt = internationalizationService.internationalize(buf.toString(), lang);

        return new File(FileType.FILE, new ByteArrayInputStream(txt.getBytes(StandardCharsets.UTF_8)), fileName + ".txt");
    }

    private String getReportStatistic(List<TrainingEvent> trainingEventList) {
        if (trainingEventList.isEmpty()) {
            return "${command.training.nostats}\n";
        }

        List<TrainingEvent> nonCanceledTrainingEventList = trainingEventList
                .stream()
                .filter(trainingEvent -> !Boolean.TRUE.equals(trainingEvent.getCanceled()))
                .toList();

        if (nonCanceledTrainingEventList.isEmpty())  {
            return "${command.training.notrainings}\n";
        }

        StringBuilder buf = new StringBuilder();

        LocalDateTime firstEventDateTime = nonCanceledTrainingEventList
                .stream()
                .min(Comparator.comparing(TrainingEvent::getDateTime))
                .orElseThrow(() -> new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)))
                .getDateTime();
        buf.append("${command.training.begginingwith} <b>").append(formatDate(firstEventDateTime)).append("</b>\n");

        int nonCanceledTrainingEventCount = nonCanceledTrainingEventList.size();
        buf.append("${command.training.trainings}: ").append(" <b>").append(nonCanceledTrainingEventCount).append("</b>\n");

        long canceledCount = trainingEventList
                .stream()
                .filter(trainingEvent -> Boolean.TRUE.equals(trainingEvent.getCanceled()))
                .count();
        buf.append("${command.training.canceledtrainings}: <b>").append(canceledCount).append("</b> (")
                .append(getPercentValue(canceledCount, nonCanceledTrainingEventCount)).append(")\n");

        long unplannedCount = nonCanceledTrainingEventList
                .stream()
                .filter(trainingEvent -> Boolean.TRUE.equals(trainingEvent.getUnplanned()))
                .count();
        buf.append("${command.training.unplannedtrainings}: <b>").append(unplannedCount).append("</b> (")
                .append(getPercentValue(unplannedCount, nonCanceledTrainingEventCount)).append(")\n");

        buf.append(BORDER);

        nonCanceledTrainingEventList
                .stream()
                .map(trainingEvent -> trainingEvent.getTraining().getName())
                .distinct()
                .forEach(name -> {
                    long trainingByNameCount = nonCanceledTrainingEventList
                            .stream()
                            .filter(trainingEvent -> name.equals(trainingEvent.getTraining().getName()))
                            .count();
                    buf.append(name).append(": <b>").append(trainingByNameCount).append("</b> (")
                            .append(getPercentValue(trainingByNameCount, nonCanceledTrainingEventCount)).append(")\n");
                });

        buf.append(BORDER);

        Duration allTrainingsDuration = nonCanceledTrainingEventList
                .stream()
                .map(trainingEvent -> getDuration(trainingEvent.getTraining().getTimeStart(), trainingEvent.getTraining().getTimeEnd()))
                .reduce(Duration::plus)
                .orElse(Duration.ZERO);
        buf.append("${command.training.totaltime}: <b>").append(DateUtils.durationToString(allTrainingsDuration)).append("</b>\n");

        Float sumOfCost = nonCanceledTrainingEventList
                .stream()
                .map(trainingEvent -> trainingEvent.getTraining().getCost())
                .reduce(Float::sum)
                .orElse(0F);
        buf.append("${command.training.totalcost}: <b>").append(sumOfCost).append("</b>\n");

        return buf.toString();
    }

    private String getMainMenuText(User user) {
        LocalDate dateNow = LocalDate.now();

        DayOfWeek dayOfWeekToday = DayOfWeek.from(dateNow);

        List<TrainingScheduled> trainingScheduledList = trainingScheduledService.get(user);
        List<org.telegram.bot.domain.entities.Training> unplannedTodayTrainings = trainingEventService.getAllUnplanned(user, dateNow)
                .stream()
                .map(TrainingEvent::getTraining)
                .toList();
        List<org.telegram.bot.domain.entities.Training> canceledTodayTrainings = trainingEventService.getAllCanceled(user, dateNow)
                .stream()
                .map(TrainingEvent::getTraining)
                .toList();

        StringBuilder buf = new StringBuilder();

        if (trainingStoppedService.isStopped(user)) {
            buf.append("<u>${command.training.schedulestopped}</u>\n\n");
        }

        buf.append("<b>${command.training.trainingstoday}:</b>\n");
        buf.append(buildFormattedTrainingList(
                getTrainingsByDayOfWeek(trainingScheduledList, unplannedTodayTrainings, canceledTodayTrainings, dayOfWeekToday)));
        buf.append("\n");
        buf.append("<b>${command.training.trainingstomorrow}:</b>\n");
        buf.append(buildFormattedTrainingList(getTrainingsByDayOfWeek(trainingScheduledList, dayOfWeekToday.plus(1))));

        buf.append(BORDER);

        TrainSubscription subscription = trainSubscriptionService.getFirstActive(user);
        buf.append("<b>${command.training.currentsubscription}:</b>");
        if (subscription != null) {
            LocalDate expirationDate = subscription.getStartDate().plus(subscription.getPeriod());
            LocalDate actualExpirationDate = calculateActualExpirationDate(subscription, trainingScheduledList);

            buf.append(" (").append(subscription.getCount()).append(")\n")
                    .append("${command.training.sellby}: <b>").append(formatDate(expirationDate)).append("</b>\n")
                    .append("${command.training.trainingsleft}: <b>").append(subscription.getCountLeft()).append("</b>\n");

            if (actualExpirationDate != null) {
                buf.append("${command.training.enoughuntil}: <b>").append(formatDate(actualExpirationDate)).append("</b>");
            }

        } else {
            buf.append("\n${command.training.notpresent}");
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
        List<Long> canceledTrainingIdList = canceledTrainingList.stream().map(org.telegram.bot.domain.entities.Training::getId).toList();
        Stream<org.telegram.bot.domain.entities.Training> plannedTrainingsTodayStream =
                getScheduledTrainingsByWeekDay(trainingScheduledList, dayOfWeek);

        return Stream.concat(plannedTrainingsTodayStream, unplannedTrainingList.stream())
                .filter(training -> !canceledTrainingIdList.contains(training.getId()))
                .sorted(Comparator.comparing(org.telegram.bot.domain.entities.Training::getTimeStart))
                .toList();
    }

    private String buildFormattedTrainingList(List<org.telegram.bot.domain.entities.Training> trainingList) {
        StringBuilder buf = new StringBuilder();

        trainingList
                .stream()
                .filter(training -> !training.getDeleted())
                .sorted(Comparator.comparing(org.telegram.bot.domain.entities.Training::getTimeStart))
                .forEach(training -> buf.append(DateUtils.formatShortTime(training.getTimeStart())).append(" — ")
                        .append(training.getName()).append(" (").append(training.getCost()).append(")").append("\n"));

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

    private Keyboard getKeyboardWithTrainingList(List<org.telegram.bot.domain.entities.Training> trainingList) {
        LocalTime timeNow = LocalTime.now();
        List<List<KeyboardButton>> rows = new ArrayList<>();
        trainingList
                .stream()
                .filter(training -> training.getTimeStart().isBefore(timeNow))
                .sorted(Comparator.comparing(org.telegram.bot.domain.entities.Training::getTimeStart))
                .forEach(training -> rows.add(List.of(
                        new KeyboardButton()
                                .setName(Emoji.NEW.getSymbol() + training.getName() + " " + formatShortTime(training.getTimeStart()) + " (" + training.getCost() + ")")
                                .setCallback(COMMAND_NAME + ADD_COMMAND + training.getId()))));

        rows.add(getTrainingMainInfoButtonRow());

        return new Keyboard(rows);
    }

    private Keyboard getReportKeyboard(User user, int page, String reportDownloadCommand) {
        List<List<KeyboardButton>> rows = new ArrayList<>();
        Page<TrainSubscription> trainSubscriptionPage = trainSubscriptionService.get(user, page);

        trainSubscriptionPage.forEach(subscription -> rows.add(List.of(
                new KeyboardButton()
                        .setName(formatDate(subscription.getStartDate()) + " — " +
                                formatDate(subscription.getStartDate().plus(subscription.getPeriod())) +
                                " (" + subscription.getCount() + ")\n")
                        .setCallback(CALLBACK_REPORT_SUBSCRIPTION_COMMAND + subscription.getId()))));

        List<KeyboardButton> pagesRow = new ArrayList<>();
        if (page > 0) {
            pagesRow.add(new KeyboardButton()
                    .setName(Emoji.LEFT_ARROW.getSymbol() + "${command.training.button.back}")
                    .setCallback(COMMAND_NAME + REPORT_COMMAND + (page - 1)));
        }

        if (page + 1 < trainSubscriptionPage.getTotalPages()) {
            pagesRow.add(new KeyboardButton()
                    .setName("${command.training.button.forward}" + Emoji.RIGHT_ARROW.getSymbol())
                    .setCallback(COMMAND_NAME + REPORT_COMMAND + (page + 1)));
        }

        rows.add(pagesRow);

        if (reportDownloadCommand != null) {
            rows.add(List.of(new KeyboardButton()
                    .setName(Emoji.DOWN_ARROW.getSymbol() + "${command.training.button.download}")
                    .setCallback(reportDownloadCommand)));
        }

        rows.add(List.of(new KeyboardButton()
                .setName("${command.training.button.forcurrentmonth}")
                .setCallback(CALLBACK_REPORT_MONTH_COMMAND)));
        rows.add(List.of(new KeyboardButton()
                .setName("${command.training.button.forcurrentyear}")
                .setCallback(CALLBACK_REPORT_YEAR_COMMAND)));
        rows.add(List.of(new KeyboardButton()
                .setName("${command.training.button.foralltime}")
                .setCallback(CALLBACK_REPORT_ALL_COMMAND)));
        rows.add(getTrainingMainInfoButtonRow());

        return new Keyboard(rows);
    }

    private List<KeyboardButton> getTrainingMainInfoButtonRow() {
        List<KeyboardButton> infoButtonRow = new ArrayList<>();
        KeyboardButton infoButton = new KeyboardButton();
        infoButton.setName(Emoji.WEIGHT_LIFTER.getSymbol() + "${command.training.button.trainings}");
        infoButton.setCallback(COMMAND_NAME);
        infoButtonRow.add(infoButton);

        return infoButtonRow;
    }

    private Keyboard getMainKeyboard() {
        return new Keyboard(
                new KeyboardButton()
                        .setName(Emoji.NEW.getSymbol() + "${command.training.button.unplannedtraining}")
                        .setCallback(COMMAND_NAME + ADD_COMMAND),
                new KeyboardButton()
                        .setName(Emoji.MEMO.getSymbol() + "${command.training.button.reports}")
                        .setCallback(COMMAND_NAME + REPORT_COMMAND),
                new KeyboardButton()
                        .setName(Emoji.GEAR.getSymbol() + "${command.training.button.settings}")
                        .setCallback("${setter.command} ${setter.set.trainings}"));
    }
}
