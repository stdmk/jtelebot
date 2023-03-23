package org.telegram.bot.domain.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.commands.Set;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.DateUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.*;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.telegram.bot.utils.DateUtils.*;
import static org.telegram.bot.utils.TextUtils.BORDER;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingSetter implements SetterParent<PartialBotApiMethod<?>> {

    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final TrainSubscriptionService trainSubscriptionService;
    private final TrainingService trainingService;
    private final TrainingScheduledService trainingScheduledService;
    private final BotStats botStats;

    private final Locale locale = new Locale("ru");
    private final String CALLBACK_COMMAND = "установить ";
    private final String EMPTY_SET_COMMAND = "тренировки";
    private final String CALLBACK_SET_COMMAND = CALLBACK_COMMAND + EMPTY_SET_COMMAND;
    private final String SET_SUBSCRIPTION_COMMAND = "sub";
    private final String DELETE_SUBSCRIPTION_COMMAND = EMPTY_SET_COMMAND + SET_SUBSCRIPTION_COMMAND + "d";
    private final String CALLBACK_DELETE_SUBSCRIPTION_COMMAND = CALLBACK_COMMAND + DELETE_SUBSCRIPTION_COMMAND;
    private final String ADD_SUBSCRIPTION_COMMAND = EMPTY_SET_COMMAND + SET_SUBSCRIPTION_COMMAND + "a";
    private final String CALLBACK_ADD_SUBSCRIPTION_COMMAND = CALLBACK_COMMAND + ADD_SUBSCRIPTION_COMMAND;
    private final String SET_TRAINING_COMMAND = "train";
    private final String DELETE_TRAINING_COMMAND = EMPTY_SET_COMMAND + SET_TRAINING_COMMAND + "d";
    private final String CALLBACK_DELETE_TRAINING_COMMAND = CALLBACK_COMMAND + DELETE_TRAINING_COMMAND;
    private final String ADD_TRAINING_COMMAND = EMPTY_SET_COMMAND + SET_TRAINING_COMMAND + "a";
    private final String CALLBACK_ADD_TRAINING_COMMAND = CALLBACK_COMMAND + ADD_TRAINING_COMMAND;
    private final String SET_SCHEDULE_COMMAND = "sch";
    private final String DELETE_SCHEDULE_COMMAND = "d";
    private final String ADD_SCHEDULE_COMMAND = "a";
    private final String SELECT_DAY_SCHEDULE_COMMAND = EMPTY_SET_COMMAND + SET_SCHEDULE_COMMAND + "w";
    private final String CALLBACK_SELECT_DAY_SCHEDULE_COMMAND = CALLBACK_COMMAND + SELECT_DAY_SCHEDULE_COMMAND;

    @Override
    public PartialBotApiMethod<?> set(Update update, String commandText) {
        Message message = getMessageFromUpdate(update);
        Chat chat = new Chat().setChatId(message.getChatId());
        String lowerCaseCommandText = commandText.toLowerCase();

        if (update.hasCallbackQuery()) {
            User user = new User().setUserId(update.getCallbackQuery().getFrom().getId());

            if (lowerCaseCommandText.equals(EMPTY_SET_COMMAND)) {
                return getSetterMainMenu(message, false);
            } else if (lowerCaseCommandText.startsWith(EMPTY_SET_COMMAND + SET_SUBSCRIPTION_COMMAND)) {
                return setSubscription(message, chat, user, commandText);
            } else if (lowerCaseCommandText.startsWith(EMPTY_SET_COMMAND + SET_TRAINING_COMMAND)) {
                return setTraining(message, chat, user, commandText);
            } else if (lowerCaseCommandText.startsWith(EMPTY_SET_COMMAND + SET_SCHEDULE_COMMAND)) {
                return setSchedule(message, chat, user, commandText);
            }
        }

        User user = new User().setUserId(message.getFrom().getId());
        if (lowerCaseCommandText.equals(EMPTY_SET_COMMAND)) {
            return getSetterMainMenu(message, true);
        }
        if (lowerCaseCommandText.startsWith(EMPTY_SET_COMMAND + SET_SUBSCRIPTION_COMMAND)) {
            return setSubscription(message, chat, user, commandText);
        } else if (lowerCaseCommandText.startsWith(EMPTY_SET_COMMAND + SET_TRAINING_COMMAND)) {
            return setTraining(message, chat, user, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private PartialBotApiMethod<?> setSubscription(Message message, Chat chat, User user, String command) {
        commandWaitingService.remove(chat, user);

        final String countAbbr = "c";
        final String dateAbbr = "da";
        final String durationAbbr = "du";

        final String setCountCommand = countAbbr + " " + "\\d+";
        final String setDateCommand = dateAbbr + " " + "(\\d{2})\\.(\\d{2})\\.(\\d{4})";
        final String setDurationCommand = durationAbbr + " " + "\\d+";

        final String setSubCountCommand = ADD_SUBSCRIPTION_COMMAND + setCountCommand;
        final String setSubCountDateCommand = setCountCommand + setDateCommand;
        final String setSubCountDateDurationCommand = setDateCommand + setDurationCommand;

        Matcher setSubCountDateDurationMatcher = Pattern.compile(setSubCountDateDurationCommand).matcher(command);
        Matcher setSubCountDateMatcher = Pattern.compile(setSubCountDateCommand).matcher(command);
        Matcher setSubCountMatcher = Pattern.compile(setSubCountCommand).matcher(command);
        Matcher setSubMatcher = Pattern.compile(ADD_SUBSCRIPTION_COMMAND).matcher(command);
        Matcher setDeleteMatcher = Pattern.compile(DELETE_SUBSCRIPTION_COMMAND + "\\d+").matcher(command);

        String responseText;
        if (setSubCountDateDurationMatcher.find()) {
            Integer count = parseCount(Pattern.compile(setCountCommand), command, countAbbr);
            LocalDate date = parseDate(Pattern.compile(setDateCommand), command, dateAbbr);
            Period period = parsePeriod(Pattern.compile(setDurationCommand), command, durationAbbr);

            TrainSubscription newTrainSubscription = new TrainSubscription()
                    .setUser(user)
                    .setCount(count)
                    .setCountLeft(Float.valueOf(count))
                    .setStartDate(date)
                    .setPeriod(period)
                    .setActive(true);

            trainSubscriptionService.getActive(user)
                    .stream()
                    .filter(trainSubscription -> trainSubscription.getCount() != 0)
                    .filter(trainSubscription -> {
                        LocalDate currentStartDate = trainSubscription.getStartDate();
                        LocalDate newStartDate = newTrainSubscription.getStartDate();
                        LocalDate currentEndDate = currentStartDate.plus(trainSubscription.getPeriod());
                        LocalDate newEndDate = newStartDate.plus(newTrainSubscription.getPeriod());

                        return (currentStartDate.isEqual(newStartDate) || currentEndDate.isEqual(newEndDate)) ||
                                //дата старта входит в период текущего абонемента
                                (currentStartDate.isBefore(newStartDate) && currentEndDate.isAfter(newStartDate)) ||
                                //дата окончания входит в период текущего абонемента
                                (currentStartDate.isBefore(newEndDate) && currentEndDate.isAfter(newEndDate)) ||
                                //новый период покрывает старый
                                (currentStartDate.isAfter(newStartDate) && currentEndDate.isBefore(newEndDate));
                    })
                    .findAny()
                    .ifPresent(trainSubscription -> {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.DUPLICATE_ENTRY));
                    });

            trainSubscriptionService.save(newTrainSubscription);

            return getManageSubscriptionMenu(message, user, true);
        } else if (setSubCountDateMatcher.find()) {
            Integer count = parseCount(Pattern.compile(setCountCommand), command, countAbbr);
            LocalDate date = parseDate(Pattern.compile(setDateCommand), command, dateAbbr);

            commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_SUBSCRIPTION_COMMAND
                    + countAbbr + " " + count
                    + dateAbbr + " " + DateUtils.formatDate(date)
                    + durationAbbr);
            responseText = "напиши мне время действия абонемента числом в календарных месяцах (1, 2, 3, 6 и т.п.)";
        } else if (setSubCountMatcher.find()) {
            Integer count = parseCount(Pattern.compile(setCountCommand), command, countAbbr);

            commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_SUBSCRIPTION_COMMAND
                    + countAbbr + " " + count
                    + dateAbbr);
            responseText = "Напиши мне дату начала действия абонемента в формате ДД.ММ.ГГГГ, " +
                    "например: <code>" + DateUtils.formatDate(LocalDate.now()) + "</code>";
        } else if (setSubMatcher.find()) {
            commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_SUBSCRIPTION_COMMAND
                    + countAbbr);
            responseText = "Напиши мне число тренировок в абонементе";
        } else if (setDeleteMatcher.find()) {
            long subscriptionId;
            try {
                subscriptionId = Integer.parseInt(command.substring(DELETE_SUBSCRIPTION_COMMAND.length()));
            } catch (NumberFormatException e) {
                botStats.incrementErrors();
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            TrainSubscription trainSubscription = trainSubscriptionService.get(subscriptionId, user);
            if (trainSubscription == null) {
                return null;
            }

            trainSubscription.setActive(false);
            trainSubscriptionService.save(trainSubscription);
            return getManageSubscriptionMenu(message, user, false);
        } else {
            return getManageSubscriptionMenu(message, user, false);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private PartialBotApiMethod<?> getManageSubscriptionMenu(Message message, User user, boolean newMessage) {
        StringBuilder buf = new StringBuilder("<b>Текущие абонементы:</b>\n");
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<TrainSubscription> trainSubscriptionList = trainSubscriptionService.getActive(user);
        if (trainSubscriptionList.isEmpty()) {
            buf.append("отсутствуют");
        } else {
            trainSubscriptionList.forEach(subscription -> {
                buf.append(DateUtils.formatDate(subscription.getStartDate())).append("\n")
                        .append("Занятий: ").append(subscription.getCount()).append("\n")
                        .append("На ").append(subscription.getPeriod().getMonths()).append(" мес.")
                        .append(" (до ").append(DateUtils.formatDate(subscription.getStartDate().plus(subscription.getPeriod()))).append(")\n\n");

                List<InlineKeyboardButton> buttonRow = new ArrayList<>();
                InlineKeyboardButton subscriptionButton = new InlineKeyboardButton();
                subscriptionButton.setText(Emoji.DELETE.getEmoji() + subscription.getStartDate() + " " + subscription.getCount());
                subscriptionButton.setCallbackData(CALLBACK_DELETE_SUBSCRIPTION_COMMAND + subscription.getId());
                buttonRow.add(subscriptionButton);

                rows.add(buttonRow);
            });
        }

        List<InlineKeyboardButton> addRow = new ArrayList<>();
        InlineKeyboardButton addSubscriptionButton = new InlineKeyboardButton();
        addSubscriptionButton.setText(Emoji.NEW.getEmoji() + "Добавить");
        addSubscriptionButton.setCallbackData(CALLBACK_ADD_SUBSCRIPTION_COMMAND);
        addRow.add(addSubscriptionButton);

        rows.add(addRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(getSetterMainMenuButtons(rows));

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.enableHtml(true);
            sendMessage.setText(buf.toString());
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(buf.toString());
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }

    private PartialBotApiMethod<?> setTraining(Message message, Chat chat, User user, String command) {
        commandWaitingService.remove(chat, user);

        command = command.replaceAll(",", ".");

        final String timeAbbr = "t";
        final String timeEndAbbr = "te";
        final String nameAbbr = "n";
        final String costAbbr = "c";

        final String setTimeCommand = timeAbbr + " " + "(\\d{2}):(\\d{2})";
        final String setTimeEndCommand = timeEndAbbr + " " + "(\\d{2}):(\\d{2})";
        final String setCostCommand = costAbbr + " " + "(\\d*\\.?\\d*)";

        final String setTrainingTimeCommand = ADD_TRAINING_COMMAND + setTimeCommand;
        final String setTrainingTimeEndCommand = setTrainingTimeCommand + setTimeEndCommand;
        final String setTrainingTimeCostCommand = setTrainingTimeEndCommand + setCostCommand;
        final String setTrainingTimeCostNameCommand = setTrainingTimeCostCommand + nameAbbr + " "  + "\\w*";

        Matcher setTrainingMatcher = Pattern.compile(ADD_TRAINING_COMMAND).matcher(command);
        Matcher setTrainingAddMatcher = Pattern.compile(setTrainingTimeCommand).matcher(command);
        Matcher setTrainingAddTimeEndMatcher = Pattern.compile(setTrainingTimeEndCommand).matcher(command);
        Matcher setTrainingAddTimeCostMatcher = Pattern.compile(setTrainingTimeCostCommand).matcher(command);
        Matcher setTrainingAddTimeCostNameMatcher = Pattern.compile(setTrainingTimeCostNameCommand).matcher(command);
        Matcher setTrainingDeleteMatcher = Pattern.compile(DELETE_TRAINING_COMMAND + "\\d+").matcher(command);

        String responseText;
        if (setTrainingAddTimeCostNameMatcher.find()) {
            LocalTime time = parseTime(Pattern.compile(setTimeCommand), command, timeAbbr);
            LocalTime timeEnd = parseTime(Pattern.compile(setTimeEndCommand), command, timeEndAbbr);
            Float cost = parseCost(Pattern.compile(setCostCommand), command, costAbbr);
            String name = command.substring(setTrainingAddTimeCostNameMatcher.end());

            if (timeEnd.isBefore(time)) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            Training training = trainingService.get(user, time, name);
            if (training != null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.DUPLICATE_ENTRY));
            }

            trainingService.save(new Training()
                    .setTimeStart(time)
                    .setTimeEnd(timeEnd)
                    .setName(name)
                    .setCost(cost)
                    .setUser(user));

            return getManageTrainingMenu(message, user, true);
        } else if (setTrainingAddTimeCostMatcher.find()) {
            LocalTime time = parseTime(Pattern.compile(setTimeCommand), command, timeAbbr);
            LocalTime timeEnd = parseTime(Pattern.compile(setTimeEndCommand), command, timeEndAbbr);
            Float cost = parseCost(Pattern.compile(setCostCommand), command, costAbbr);

            responseText = "Напиши мне наименование тренировки";
            commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_TRAINING_COMMAND
                    + timeAbbr + " " + formatShortTime(time)
                    + timeEndAbbr + " " + formatShortTime(timeEnd)
                    + costAbbr + " " + cost
                    + nameAbbr);
        } else if (setTrainingAddTimeEndMatcher.find()) {
            LocalTime time = parseTime(Pattern.compile(setTimeCommand), command, timeAbbr);
            LocalTime timeEnd = parseTime(Pattern.compile(setTimeEndCommand), command, timeEndAbbr);

            commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_TRAINING_COMMAND
                    + timeAbbr + " " + formatShortTime(time)
                    + timeEndAbbr + " " + formatShortTime(timeEnd)
                    + costAbbr);
            responseText = "Напиши мне стоимость тренировки (1 — одно занятие, 0,5 — половина занятия, 2 и т.п.)";
        } else if (setTrainingAddMatcher.find()) {
            LocalTime time = parseTime(Pattern.compile(setTimeCommand), command, timeAbbr);

            responseText = "Напиши мне время окончания тренировки в формате ЧЧ:ММ. " +
                    "Например: " + formatShortTime(LocalTime.now());
            commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_TRAINING_COMMAND
                    + timeAbbr + " " + formatShortTime(time)
                    + timeEndAbbr);
        } else if (setTrainingMatcher.find()) {
            responseText = "Напиши мне время начала тренировки в формате ЧЧ:ММ. " +
                    "Например: " + formatShortTime(LocalTime.now());
            commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_TRAINING_COMMAND
                    + timeAbbr);
        } else if (setTrainingDeleteMatcher.find()) {
            long trainingId;
            try {
                trainingId = Integer.parseInt(command.substring(DELETE_TRAINING_COMMAND.length()));
            } catch (NumberFormatException e) {
                botStats.incrementErrors();
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            Training training = trainingService.get(user, trainingId);
            if (training == null) {
                return null;
            }

            trainingScheduledService.removeAllByTraining(training);
            trainingService.remove(training);

            return getManageTrainingMenu(message, user, false);
        } else {
            return getManageTrainingMenu(message, user, false);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private PartialBotApiMethod<?> getManageTrainingMenu(Message message, User user, boolean newMessage) {
        StringBuilder buf = new StringBuilder("<b>Текущие тренировки:</b>\n");
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<Training> trainingList = trainingService.get(user);
        if (trainingList.isEmpty()) {
            buf.append("отсутствуют");
        } else {
            trainingList.forEach(training -> {
                buf.append(training.getName()).append(" — ").append(formatShortTime(training.getTimeStart())).append(" (").append(training.getCost()).append(")\n");

                List<InlineKeyboardButton> buttonRow = new ArrayList<>();
                InlineKeyboardButton trainingButton = new InlineKeyboardButton();
                trainingButton.setText(Emoji.DELETE.getEmoji() + training.getName() + " " + formatShortTime(training.getTimeStart()));
                trainingButton.setCallbackData(CALLBACK_DELETE_TRAINING_COMMAND + training.getId());
                buttonRow.add(trainingButton);

                rows.add(buttonRow);
            });
        }

        List<InlineKeyboardButton> addRow = new ArrayList<>();
        InlineKeyboardButton addTrainingButton = new InlineKeyboardButton();
        addTrainingButton.setText(Emoji.NEW.getEmoji() + "Добавить");
        addTrainingButton.setCallbackData(CALLBACK_ADD_TRAINING_COMMAND);
        addRow.add(addTrainingButton);

        rows.add(addRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(getSetterMainMenuButtons(rows));

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.enableHtml(true);
            sendMessage.setText(buf.toString());
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(buf.toString());
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }
    private EditMessageText setSchedule(Message message, Chat chat, User user, String command) {
        commandWaitingService.remove(chat, user);

        final String dayWeekAbbr = "w";
        final String addTrainingAbbr = "a";
        final String removeTrainingAbbr = "d";

        final String selectDayOfWeekCommand = dayWeekAbbr + "\\d+";
        final String addTrainingCommand = addTrainingAbbr + "\\d+";
        final String removeTrainingCommand = removeTrainingAbbr + "\\d+";

        final String selectWeekDayCommand = SET_SCHEDULE_COMMAND + selectDayOfWeekCommand;
        final String selectWeekDayAddTrainingCommand = selectWeekDayCommand + addTrainingCommand;
        final String selectWeekDayRemoveTrainingCommand = selectWeekDayCommand + removeTrainingCommand;

        Matcher setScheduleSelectWeekDayMatcher = Pattern.compile(selectWeekDayCommand).matcher(command);
        Matcher setScheduleSelectWeekDayAddTrainingMatcher = Pattern.compile(selectWeekDayAddTrainingCommand).matcher(command);
        Matcher setScheduleSelectWeekDayRemoveTrainingMatcher = Pattern.compile(selectWeekDayRemoveTrainingCommand).matcher(command);

        if (setScheduleSelectWeekDayAddTrainingMatcher.find()) {
            DayOfWeek dayOfWeek = parseDayOfWeek(Pattern.compile(selectDayOfWeekCommand), command, dayWeekAbbr);
            Long trainingId = parseLong(Pattern.compile(addTrainingCommand), command, addTrainingAbbr);
            Training training = trainingService.get(user, trainingId);

            if (training == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            trainingScheduledService.get(user, dayOfWeek)
                    .stream()
                    .filter(trainingScheduled -> training.getTimeStart().equals(trainingScheduled.getTraining().getTimeStart()))
                    .findFirst()
                    .ifPresent(trainingScheduled -> {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    });

            trainingScheduledService.save(new TrainingScheduled()
                    .setUser(user)
                    .setTraining(training)
                    .setDayOfWeek(dayOfWeek));

            return getManageScheduleByDayOfWeekMenu(message, user, dayOfWeek);
        } else if (setScheduleSelectWeekDayRemoveTrainingMatcher.find()) {
            DayOfWeek dayOfWeek = parseDayOfWeek(Pattern.compile(selectDayOfWeekCommand), command, dayWeekAbbr);
            Long trainingId = parseLong(Pattern.compile(removeTrainingCommand), command, removeTrainingAbbr);
            Training training = new Training().setId(trainingId);

            TrainingScheduled trainingScheduled = trainingScheduledService.get(user, dayOfWeek, training);
            if (trainingScheduled == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            trainingScheduledService.remove(trainingScheduled);

            return getManageScheduleByDayOfWeekMenu(message, user, dayOfWeek);
        } else if (setScheduleSelectWeekDayMatcher.find()) {
            DayOfWeek dayOfWeek = parseDayOfWeek(Pattern.compile(selectDayOfWeekCommand), command, dayWeekAbbr);
            return getManageScheduleByDayOfWeekMenu(message, user, dayOfWeek);
        } else {
            return getManageScheduleMenu(message, user);
        }
    }

    private EditMessageText getManageScheduleMenu(Message message, User user) {
        StringBuilder buf = new StringBuilder("<b>Текущие тренировки:</b>\n");
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> daysOfWeekRow = new ArrayList<>();

        List<TrainingScheduled> trainingScheduledList = trainingScheduledService.get(user);
        if (trainingScheduledList.isEmpty()) {
            buf.append("отсутствуют");
        } else {
            Arrays.stream(DayOfWeek.values()).forEach(dayOfWeek -> {
                buf.append("<b>").append(dayOfWeek.getDisplayName(TextStyle.FULL, locale)).append("</b>\n");
                trainingScheduledList
                        .stream()
                        .filter(trainingScheduled -> dayOfWeek.equals(trainingScheduled.getDayOfWeek()))
                        .forEach(trainingScheduled -> buf.append(trainingScheduled.getTraining().getTimeStart()).append(" — ")
                                .append(trainingScheduled.getTraining().getName()).append("\n"));
                buf.append(BORDER);
            });
        }

        Arrays.stream(DayOfWeek.values()).forEach(dayOfWeek -> {
            InlineKeyboardButton dayOfWeekButton = new InlineKeyboardButton();
            dayOfWeekButton.setText(dayOfWeek.getDisplayName(TextStyle.SHORT, locale));
            dayOfWeekButton.setCallbackData(CALLBACK_SELECT_DAY_SCHEDULE_COMMAND + dayOfWeek.getValue());
            daysOfWeekRow.add(dayOfWeekButton);
        });

        rows.add(daysOfWeekRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(getSetterMainMenuButtons(rows));

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(buf.toString());
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }

    private EditMessageText getManageScheduleByDayOfWeekMenu(Message message, User user, DayOfWeek dayOfWeek) {
        StringBuilder buf = new StringBuilder("<b>" + dayOfWeek.getDisplayName(TextStyle.FULL, locale) + "</b>\n");
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        final String selectDayOfWeekCallback = CALLBACK_SELECT_DAY_SCHEDULE_COMMAND + dayOfWeek.getValue();

        List<Long> alreadySelectedTrainingIdList = new ArrayList<>();
        List<Training> userTrainings = trainingService.get(user);
        List<TrainingScheduled> trainingScheduledList = trainingScheduledService.get(user, dayOfWeek);
        if (trainingScheduledList.isEmpty()) {
            buf.append("отсутствуют");
        } else {
            trainingScheduledList.forEach(trainingScheduled -> {
                Training training = trainingScheduled.getTraining();
                String caption = training.getTimeStart() + " — " + training.getName();

                buf.append(caption).append("\n");

                List<InlineKeyboardButton> buttonRow = new ArrayList<>();
                InlineKeyboardButton removeButton = new InlineKeyboardButton();
                removeButton.setText(Emoji.DELETE.getEmoji() + caption);
                removeButton.setCallbackData(selectDayOfWeekCallback + DELETE_SCHEDULE_COMMAND + training.getId());
                buttonRow.add(removeButton);

                rows.add(buttonRow);

                alreadySelectedTrainingIdList.add(training.getId());
            });
        }

        userTrainings.forEach(training -> {
            if (alreadySelectedTrainingIdList.contains(training.getId())) {
                return;
            }

            List<InlineKeyboardButton> buttonRow = new ArrayList<>();
            InlineKeyboardButton addButton = new InlineKeyboardButton();
            addButton.setText(Emoji.NEW.getEmoji() + training.getTimeStart() + " — " + training.getName());
            addButton.setCallbackData(selectDayOfWeekCallback + ADD_SCHEDULE_COMMAND + training.getId());
            buttonRow.add(addButton);
            rows.add(buttonRow);
        });

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(getSetterMainMenuButtons(rows));

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(buf.toString());
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }

    private PartialBotApiMethod<?> getSetterMainMenu(Message message, boolean newMessage) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        String caption = "<b>Установки тренировок:</b>";

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(getSetterMainMenuButtons(rows));

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.enableHtml(true);
            sendMessage.setText(caption);
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(caption);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }

    private List<List<InlineKeyboardButton>> getSetterMainMenuButtons(List<List<InlineKeyboardButton>> rows) {
        List<InlineKeyboardButton> subscriptionButtonRow = new ArrayList<>();
        InlineKeyboardButton subscriptionButton = new InlineKeyboardButton();
        subscriptionButton.setText(Emoji.TICKET.getEmoji() + "Абонемент");
        subscriptionButton.setCallbackData(CALLBACK_SET_COMMAND + SET_SUBSCRIPTION_COMMAND);
        subscriptionButtonRow.add(subscriptionButton);

        List<InlineKeyboardButton> trainingButtonRow = new ArrayList<>();
        InlineKeyboardButton trainingButton = new InlineKeyboardButton();
        trainingButton.setText(Emoji.GREEN_BOOK.getEmoji() + "Номенклатура");
        trainingButton.setCallbackData(CALLBACK_SET_COMMAND + SET_TRAINING_COMMAND);
        trainingButtonRow.add(trainingButton);

        List<InlineKeyboardButton> scheduleButtonRow = new ArrayList<>();
        InlineKeyboardButton scheduleButton = new InlineKeyboardButton();
        scheduleButton.setText(Emoji.DATE.getEmoji() + "Расписание");
        scheduleButton.setCallbackData(CALLBACK_SET_COMMAND + SET_SCHEDULE_COMMAND);
        scheduleButtonRow.add(scheduleButton);

        List<InlineKeyboardButton> infoButtonRow = new ArrayList<>();
        InlineKeyboardButton infoButton = new InlineKeyboardButton();
        infoButton.setText(Emoji.WEIGHT_LIFTER.getEmoji() + "Тренировки");
        infoButton.setCallbackData("training");
        infoButtonRow.add(infoButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getEmoji() + "Установки");
        backButton.setCallbackData(CALLBACK_COMMAND + "back");
        backButtonRow.add(backButton);

        rows.add(subscriptionButtonRow);
        rows.add(trainingButtonRow);
        rows.add(scheduleButtonRow);
        rows.add(infoButtonRow);
        rows.add(backButtonRow);

        return rows;
    }

    private Integer parseCount(Pattern pattern, String text, String abbr) {
        Matcher matcher = pattern.matcher(text);
        Integer count = parseFromMatcher(matcher, () -> Integer.parseInt(text.substring(matcher.start() + abbr.length() + 1, matcher.end())));

        if (count < 1) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return count;
    }

    private Long parseLong(Pattern pattern, String text, String abbr) {
        Matcher matcher = pattern.matcher(text);
        return parseFromMatcher(matcher, () -> Long.parseLong(text.substring(matcher.start() + abbr.length(), matcher.end())));
    }

    private Float parseCost(Pattern pattern, String text, String abbr) {
        Matcher matcher = pattern.matcher(text);
        Float cost = parseFromMatcher(matcher, () -> Float.parseFloat(text.substring(matcher.start() + abbr.length() + 1, matcher.end())));

        if (cost < 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return cost;
    }

    private LocalDate parseDate(Pattern pattern, String text, String abbr) {
        Matcher matcher = pattern.matcher(text);
        return parseFromMatcher(matcher, () -> LocalDate.parse(text.substring(matcher.start() + abbr.length() + 1, matcher.end()), dateFormatter));
    }

    private LocalTime parseTime(Pattern pattern, String text, String abbr) {
        Matcher matcher = pattern.matcher(text);
        return parseFromMatcher(matcher, () -> LocalTime.parse(text.substring(matcher.start() + abbr.length() + 1, matcher.end()), timeShortFormatter));
    }

    private Period parsePeriod(Pattern pattern, String text, String abbr) {
        Matcher matcher = pattern.matcher(text);
        return parseFromMatcher(matcher, () -> Period.ofMonths(Integer.parseInt(text.substring(matcher.start() + abbr.length() + 1, matcher.end()))));
    }

    private DayOfWeek parseDayOfWeek(Pattern pattern, String text, String abbr) {
        Matcher matcher = pattern.matcher(text);
        return parseFromMatcher(matcher, () -> DayOfWeek.of(Integer.parseInt(text.substring(matcher.start() + abbr.length(), matcher.end()))));
    }

    private <T> T parseFromMatcher(Matcher matcher, Supplier<T> parse) {
        if (matcher.find()) {
            try {
                return parse.get();
            } catch (Exception e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        } else {
            botStats.incrementErrors();
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }
}
