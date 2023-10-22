package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.DateUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.telegram.bot.utils.DateUtils.*;
import static org.telegram.bot.utils.MathUtils.getPercentValue;
import static org.telegram.bot.utils.TextUtils.BORDER;
import static org.telegram.bot.utils.TextUtils.cutHtmlTags;

@Component
@RequiredArgsConstructor
public class Training implements Command<PartialBotApiMethod<?>> {

    private final Bot bot;
    private final TrainingScheduledService trainingScheduledService;
    private final TrainSubscriptionService trainSubscriptionService;
    private final TrainingEventService trainingEventService;
    private final TrainingService trainingService;
    private final TrainingStoppedService trainingStoppedService;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final LanguageResolver languageResolver;

    private final Locale LOCALE = new Locale("ru");

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
    public PartialBotApiMethod<?> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        Long userId;
        Integer editMessageId = null;

        if (update.hasCallbackQuery()) {
            userId = update.getCallbackQuery().getFrom().getId();
            editMessageId = message.getMessageId();
        } else {
            userId = message.getFrom().getId();
        }

        User user = new User().setUserId(userId);
        Chat chat = new Chat().setChatId(message.getChatId());

        String textMessage;
        CommandWaiting commandWaiting = commandWaitingService.get(chat, user);
        if (update.hasCallbackQuery()) {
            textMessage = cutCommandInText(update.getCallbackQuery().getData());
        } else if (commandWaiting != null) {
            textMessage = cutCommandInText(commandWaiting.getTextMessage() + message.getText());
        } else {
            textMessage = cutCommandInText(message.getText());
        }

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
                    responseText = "<b>${command.training.choosetraining}:</b>";
                    List<org.telegram.bot.domain.entities.Training> trainingList = trainingService.get(user);
                    if (trainingList.isEmpty()) {
                        responseText = responseText + "${command.training.emptynomenclature} /set";
                        inlineKeyboardMarkup = getMainKeyboard();
                    } else {
                        inlineKeyboardMarkup = getKeyboardWithTrainingList(trainingList);
                    }
                }
            } else if (textMessage.startsWith(CANCEL_COMMAND)) {
                commandWaitingService.remove(chat, user);

                String cancellationReason = null;
                String eventIdString;
                if (textMessage.contains(CANCEL_REASON_COMMAND)) {
                    int cancellationReasonCommandIndex = textMessage.indexOf(CANCEL_REASON_COMMAND);
                    eventIdString = textMessage.substring(CANCEL_COMMAND.length(), cancellationReasonCommandIndex);
                    cancellationReason = textMessage.substring(cancellationReasonCommandIndex + 4);
                } else {
                    eventIdString = textMessage.substring(CANCEL_COMMAND.length());
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
            } else if (textMessage.startsWith(REPORT_COMMAND)) {
                int page = 0;
                LocalDate dateNow = LocalDate.now();
                String reportDownloadCommand = null;

                if (textMessage.startsWith(REPORT_ALL_TIME_COMMAND)) {
                    responseText = getReportStatistic(trainingEventService.getAll(user));
                    reportDownloadCommand = CALLBACK_DOWNLOAD_REPORT_ALL_COMMAND;
                } else if (textMessage.startsWith(REPORT_YEAR_COMMAND)) {
                    responseText = getReportStatistic(trainingEventService.getAllOfYear(user, dateNow.getYear()));
                    reportDownloadCommand = CALLBACK_DOWNLOAD_REPORT_YEAR_COMMAND;
                } else if (textMessage.startsWith(REPORT_MONTH_COMMAND)) {
                    responseText = getReportStatistic(trainingEventService.getAllOfMonth(user, dateNow.getMonthValue()));
                    reportDownloadCommand = CALLBACK_DOWNLOAD_REPORT_MONTH_COMMAND;
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
                    reportDownloadCommand = CALLBACK_DOWNLOAD_REPORT_SUBSCRIPTION_COMMAND + subscriptionId;
                } else {
                    try {
                        page = Integer.parseInt(textMessage.substring(REPORT_COMMAND.length()));
                    } catch (NumberFormatException ignored) {
                    }

                    responseText = "<b>${command.training.choosereport}:</b>\n";
                }

                inlineKeyboardMarkup = getReportKeyboard(user, page, reportDownloadCommand);
            } else if (textMessage.startsWith(DOWNLOAD_COMMAND)) {
                LocalDate dateNow = LocalDate.now();
                String languageCode = languageResolver.getChatLanguageCode(message);
                InputFile inputFile;
                String caption;

                if (textMessage.startsWith(DOWNLOAD_REPORT_ALL_TIME_COMMAND)) {
                    inputFile = getReportFile(trainingEventService.getAll(user), "all", languageCode);
                    caption = "{command.training.alltimereport}";
                } else if (textMessage.startsWith(DOWNLOAD_REPORT_YEAR_COMMAND)) {
                    int year = dateNow.getYear();
                    inputFile = getReportFile(trainingEventService.getAllOfYear(user, year), String.valueOf(year), languageCode);
                    caption = "{command.training.yearreport} " + year;
                } else if (textMessage.startsWith(DOWNLOAD_REPORT_MONTH_COMMAND)) {
                    String monthName = dateNow.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, LOCALE);
                    inputFile = getReportFile(trainingEventService.getAllOfMonth(user, dateNow.getMonthValue()), monthName, languageCode);
                    caption = "{command.training.monthly} " + monthName;
                } else if (textMessage.startsWith(DOWNLOAD_REPORT_SUBSCRIPTION_COMMAND)) {
                    long subscriptionId;
                    try {
                        subscriptionId = Long.parseLong(textMessage.substring(DOWNLOAD_REPORT_SUBSCRIPTION_COMMAND.length()));
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
                    inputFile = getReportFile(trainingEventService.getAll(user, subscription), subscriptionName, languageCode);
                    caption = "${command.training.subscriptionreport} " + subscriptionName;
                } else {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                }

                SendDocument sendDocument = new SendDocument();
                sendDocument.setChatId(message.getChatId().toString());
                sendDocument.setReplyToMessageId(message.getMessageId());
                sendDocument.setCaption(caption);
                sendDocument.setDocument(inputFile);

                return sendDocument;
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

    private InputFile getReportFile(List<TrainingEvent> trainingEventList, String fileName, String lang) {
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

        return new InputFile(new ByteArrayInputStream(buf.toString().getBytes(StandardCharsets.UTF_8)), fileName + ".txt");
    }

    private String getReportStatistic(List<TrainingEvent> trainingEventList) {
        if (trainingEventList.isEmpty()) {
            return "${command.training.nostats}\n";
        }

        List<TrainingEvent> nonCanceledTrainingEventList = trainingEventList
                .stream()
                .filter(trainingEvent -> !Boolean.TRUE.equals(trainingEvent.getCanceled()))
                .collect(Collectors.toList());

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
                .collect(Collectors.toList());
        List<org.telegram.bot.domain.entities.Training> canceledTodayTrainings = trainingEventService.getAllCanceled(user, dateNow)
                .stream()
                .map(TrainingEvent::getTraining)
                .collect(Collectors.toList());

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
                    trainingButton.setText(Emoji.NEW.getEmoji() + training.getName() + " " + formatShortTime(training.getTimeStart()) + " (" + training.getCost() + ")");
                    trainingButton.setCallbackData(COMMAND_NAME + ADD_COMMAND + training.getId());
                    buttonRow.add(trainingButton);

                    rows.add(buttonRow);
        });

        rows.add(getTrainingMainInfoButtonRow());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup getReportKeyboard(User user, int page, String reportDownloadCommand) {
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
            backButton.setText(Emoji.LEFT_ARROW.getEmoji() + "${command.training.button.back}");
            backButton.setCallbackData(COMMAND_NAME + REPORT_COMMAND + (page - 1));

            pagesRow.add(backButton);
        }

        if (page + 1 < trainSubscriptionPage.getTotalPages()) {
            InlineKeyboardButton forwardButton = new InlineKeyboardButton();
            forwardButton.setText("${command.training.button.forward}" + Emoji.RIGHT_ARROW.getEmoji());
            forwardButton.setCallbackData(COMMAND_NAME + REPORT_COMMAND + (page + 1));

            pagesRow.add(forwardButton);
        }

        rows.add(pagesRow);

        if (reportDownloadCommand != null) {
            List<InlineKeyboardButton> downloadReportTrainingRow = new ArrayList<>();
            InlineKeyboardButton downloadReportButton = new InlineKeyboardButton();
            downloadReportButton.setText(Emoji.DOWN_ARROW.getEmoji() + "${${command.training.button.download}}");
            downloadReportButton.setCallbackData(reportDownloadCommand);
            downloadReportTrainingRow.add(downloadReportButton);

            rows.add(downloadReportTrainingRow);
        }

        List<InlineKeyboardButton> reportForMonthRow = new ArrayList<>();
        InlineKeyboardButton monthReportButton = new InlineKeyboardButton();
        monthReportButton.setText("${command.training.button.forcurrentmonth}");
        monthReportButton.setCallbackData(CALLBACK_REPORT_MONTH_COMMAND);
        reportForMonthRow.add(monthReportButton);

        List<InlineKeyboardButton> reportForYearRow = new ArrayList<>();
        InlineKeyboardButton yearReportButton = new InlineKeyboardButton();
        yearReportButton.setText("${command.training.button.forcurrentyear}");
        yearReportButton.setCallbackData(CALLBACK_REPORT_YEAR_COMMAND);
        reportForYearRow.add(yearReportButton);

        List<InlineKeyboardButton> reportForAllTimeRow = new ArrayList<>();
        InlineKeyboardButton allTimeReportButton = new InlineKeyboardButton();
        allTimeReportButton.setText("${command.training.button.foralltime}");
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
        infoButton.setText(Emoji.WEIGHT_LIFTER.getEmoji() + "${command.training.button.trainings}");
        infoButton.setCallbackData(COMMAND_NAME);
        infoButtonRow.add(infoButton);
        
        return infoButtonRow;
    }

    private InlineKeyboardMarkup getMainKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> addTrainingRow = new ArrayList<>();
        InlineKeyboardButton trainingButton = new InlineKeyboardButton();
        trainingButton.setText(Emoji.NEW.getEmoji() + "${command.training.button.unplannedtraining}");
        trainingButton.setCallbackData(COMMAND_NAME + ADD_COMMAND);
        addTrainingRow.add(trainingButton);

        List<InlineKeyboardButton> reportTrainingRow = new ArrayList<>();
        InlineKeyboardButton reportButton = new InlineKeyboardButton();
        reportButton.setText(Emoji.MEMO.getEmoji() + "${command.training.button.reports}");
        reportButton.setCallbackData(COMMAND_NAME + REPORT_COMMAND);
        reportTrainingRow.add(reportButton);

        List<InlineKeyboardButton> settingsRow = new ArrayList<>();
        InlineKeyboardButton settingsButton = new InlineKeyboardButton();
        settingsButton.setText(Emoji.GEAR.getEmoji() + "${command.training.button.settings}");
        settingsButton.setCallbackData("${setter.command} ${setter.set.trainings}");
        settingsRow.add(settingsButton);

        rows.add(addTrainingRow);
        rows.add(reportTrainingRow);
        rows.add(settingsRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }
}
