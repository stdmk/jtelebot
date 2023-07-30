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
import java.util.*;
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
    private final TrainingStoppedService trainingStoppedService;
    private final BotStats botStats;

    private static final Locale LOCALE = new Locale("ru");
    private static final String CALLBACK_COMMAND = "установить ";
    private static final String EMPTY_SET_COMMAND = "тренировки";
    private static final String CALLBACK_SET_COMMAND = CALLBACK_COMMAND + EMPTY_SET_COMMAND;
    private static final String SET_SUBSCRIPTION_COMMAND = "sub";
    private static final String DELETE_SUBSCRIPTION_COMMAND = EMPTY_SET_COMMAND + SET_SUBSCRIPTION_COMMAND + "d";
    private static final String CALLBACK_DELETE_SUBSCRIPTION_COMMAND = CALLBACK_COMMAND + DELETE_SUBSCRIPTION_COMMAND;
    private static final String ADD_SUBSCRIPTION_COMMAND = EMPTY_SET_COMMAND + SET_SUBSCRIPTION_COMMAND + "a";
    private static final String CALLBACK_ADD_SUBSCRIPTION_COMMAND = CALLBACK_COMMAND + ADD_SUBSCRIPTION_COMMAND;
    private static final String UPDATE_SUBSCRIPTION_COMMAND = EMPTY_SET_COMMAND + SET_SUBSCRIPTION_COMMAND + "u";
    private static final String CALLBACK_UPDATE_SUBSCRIPTION_COMMAND = CALLBACK_COMMAND + UPDATE_SUBSCRIPTION_COMMAND;
    private static final String SET_TRAINING_COMMAND = "train";
    private static final String DELETE_TRAINING_COMMAND = EMPTY_SET_COMMAND + SET_TRAINING_COMMAND + "d";
    private static final String CALLBACK_DELETE_TRAINING_COMMAND = CALLBACK_COMMAND + DELETE_TRAINING_COMMAND;
    private static final String ADD_TRAINING_COMMAND = EMPTY_SET_COMMAND + SET_TRAINING_COMMAND + "a";
    private static final String CALLBACK_ADD_TRAINING_COMMAND = CALLBACK_COMMAND + ADD_TRAINING_COMMAND;
    private static final String SET_SCHEDULE_COMMAND = "sch";
    private static final String DELETE_SCHEDULE_COMMAND = "d";
    private static final String ADD_SCHEDULE_COMMAND = "a";
    private static final String STOP_SCHEDULE_COMMAND = EMPTY_SET_COMMAND + SET_SCHEDULE_COMMAND + "s";
    private static final String CALLBACK_STOP_SCHEDULE_COMMAND = CALLBACK_COMMAND + STOP_SCHEDULE_COMMAND;
    private static final String SELECT_DAY_SCHEDULE_COMMAND = EMPTY_SET_COMMAND + SET_SCHEDULE_COMMAND + "w";
    private static final String CALLBACK_SELECT_DAY_SCHEDULE_COMMAND = CALLBACK_COMMAND + SELECT_DAY_SCHEDULE_COMMAND;
    private static final String COUNT_ABBR = "c";
    private static final String DATE_ABBR = "da";
    private static final String DURATION_ABBR = "du";
    private static final String TIME_ABBR = "t";
    private static final String TIME_END_ABBR = "te";
    private static final String NAME_ABBR = "n";
    private static final String COST_ABBR = "c";
    private static final String DAY_WEEK_ABBR = "w";
    private static final String ADD_TRAINING_ABBR = "a";
    private static final String REMOVE_TRAINING_ABBR = "d";
    private static final String SET_COUNT_COMMAND = COUNT_ABBR + " " + "\\d+";
    private static final String SET_DATE_COMMAND = DATE_ABBR + " " + "(\\d{2})\\.(\\d{2})\\.(\\d{4})";
    private static final String SET_DURATION_COMMAND = DURATION_ABBR + " " + "\\d+";
    private static final String SET_SUB_COUNT_COMMAND = ADD_SUBSCRIPTION_COMMAND + SET_COUNT_COMMAND;
    private static final String SET_SUB_COUNT_DATE_COMMAND = SET_COUNT_COMMAND + SET_DATE_COMMAND;
    private static final String SET_SUB_COUNT_DATE_DURATION_COMMAND = SET_DATE_COMMAND + SET_DURATION_COMMAND;
    private static final String SET_TIME_COMMAND = TIME_ABBR + " " + "(\\d{2}):(\\d{2})";
    private static final String SET_TIME_END_COMMAND = TIME_END_ABBR + " " + "(\\d{2}):(\\d{2})";
    private static final String SET_COST_COMMAND = COST_ABBR + " " + "(\\d*\\.?\\d*)";
    private static final String SET_TRAINING_TIME_COMMAND = ADD_TRAINING_COMMAND + SET_TIME_COMMAND;
    private static final String SET_TRAINING_TIME_END_COMMAND = SET_TRAINING_TIME_COMMAND + SET_TIME_END_COMMAND;
    private static final String SET_TRAINING_TIME_COST_COMMAND = SET_TRAINING_TIME_END_COMMAND + SET_COST_COMMAND;
    private static final String SET_TRAINING_TIME_COST_NAME_COMMAND = SET_TRAINING_TIME_COST_COMMAND + NAME_ABBR + " "  + "\\w*";
    private static final String SELECT_DAY_OF_WEEK_COMMAND = DAY_WEEK_ABBR + "\\d+";
    private static final String ADD_TRAINING_ENTITY_COMMAND = ADD_TRAINING_ABBR + "\\d+";
    private static final String REMOVE_TRAINING_ENTITY_COMMAND = REMOVE_TRAINING_ABBR + "\\d+";
    private static final String SELECT_WEEK_DAY_COMMAND = SET_SCHEDULE_COMMAND + SELECT_DAY_OF_WEEK_COMMAND;
    private static final String SELECT_WEEK_DAY_ADD_TRAINING_COMMAND = SELECT_WEEK_DAY_COMMAND + ADD_TRAINING_ENTITY_COMMAND;
    private static final String SELECT_WEEK_DAY_REMOVE_TRAINING_COMMAND = SELECT_WEEK_DAY_COMMAND + REMOVE_TRAINING_ENTITY_COMMAND;

    private static final Pattern SET_SUB_COUNT_DATE_DURATION_COMMAND_PATTERN = Pattern.compile(SET_SUB_COUNT_DATE_DURATION_COMMAND);
    private static final Pattern SET_SUB_COUNT_DATE_COMMAND_PATTERN = Pattern.compile(SET_SUB_COUNT_DATE_COMMAND);
    private static final Pattern SET_SUB_COUNT_COMMAND_PATTERN = Pattern.compile(SET_SUB_COUNT_COMMAND);
    private static final Pattern SET_SUB_PATTERN = Pattern.compile(ADD_SUBSCRIPTION_COMMAND);
    private static final Pattern SET_DELETE_SUB_PATTERN = Pattern.compile(DELETE_SUBSCRIPTION_COMMAND + "\\d+");
    private static final Pattern UPDATE_SUBSCRIPTION_COMMAND_PATTERN = Pattern.compile(UPDATE_SUBSCRIPTION_COMMAND + "\\d+");
    private static final Pattern UPDATE_DATE_END_PATTERN = Pattern.compile(UPDATE_SUBSCRIPTION_COMMAND + "\\d+" + DATE_ABBR + " (\\d{2})\\.(\\d{2})\\.(\\d{4})");
    private static final Pattern SET_COUNT_PATTERN =  Pattern.compile(SET_COUNT_COMMAND);
    private static final Pattern SET_DATE_PATTERN = Pattern.compile(SET_DATE_COMMAND);
    private static final Pattern SET_DURATION_PATTERN = Pattern.compile(SET_DURATION_COMMAND);
    private static final Pattern UPDATE_SUB_PATTERN = Pattern.compile(UPDATE_SUBSCRIPTION_COMMAND + "\\d");
    private static final Pattern SET_TRAINING_PATTERN = Pattern.compile(ADD_TRAINING_COMMAND);
    private static final Pattern SET_TRAINING_ADD_PATTERN = Pattern.compile(SET_TRAINING_TIME_COMMAND);
    private static final Pattern SET_TRAINING_ADD_TIME_END_PATTERN = Pattern.compile(SET_TRAINING_TIME_END_COMMAND);
    private static final Pattern SET_TRAINING_ADD_TIME_COST_PATTERN = Pattern.compile(SET_TRAINING_TIME_COST_COMMAND);
    private static final Pattern SET_TRAINING_ADD_TIME_COST_NAME_PATTERN = Pattern.compile(SET_TRAINING_TIME_COST_NAME_COMMAND);
    private static final Pattern SET_TRAINING_DELETE_PATTERN = Pattern.compile(DELETE_TRAINING_COMMAND + "\\d+");
    private static final Pattern SET_TIME_COMMAND_PATTERN = Pattern.compile(SET_TIME_COMMAND);
    private static final Pattern SET_TIME_END_COMMAND_PATTERN = Pattern.compile(SET_TIME_END_COMMAND);
    private static final Pattern SET_COST_COMMAND_PATTERN = Pattern.compile(SET_COST_COMMAND);
    private static final Pattern SET_SCHEDULE_SELECT_WEEK_DAY_PATTERN = Pattern.compile(SELECT_WEEK_DAY_COMMAND);
    private static final Pattern SET_SCHEDULE_SELECT_WEEK_DAY_ADD_TRAINING_PATTERN = Pattern.compile(SELECT_WEEK_DAY_ADD_TRAINING_COMMAND);
    private static final Pattern SET_SCHEDULE_SELECT_WEEK_DAY_REMOVE_TRAINING_PATTERN = Pattern.compile(SELECT_WEEK_DAY_REMOVE_TRAINING_COMMAND);
    private static final Pattern SELECT_DAY_OF_WEEK_PATTERN =  Pattern.compile(SELECT_DAY_OF_WEEK_COMMAND);
    private static final Pattern ADD_TRAINING_ENTITY_COMMAND_PATTERN = Pattern.compile(ADD_TRAINING_ENTITY_COMMAND);
    private static final Pattern REMOVE_TRAINING_ENTITY_COMMAND_PATTERN = Pattern.compile(REMOVE_TRAINING_ENTITY_COMMAND);

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

        Matcher setSubCountDateDurationMatcher = SET_SUB_COUNT_DATE_DURATION_COMMAND_PATTERN.matcher(command);
        Matcher setSubCountDateMatcher = SET_SUB_COUNT_DATE_COMMAND_PATTERN.matcher(command);
        Matcher setSubCountMatcher = SET_SUB_COUNT_COMMAND_PATTERN.matcher(command);
        Matcher setSubMatcher = SET_SUB_PATTERN.matcher(command);
        Matcher setDeleteMatcher = SET_DELETE_SUB_PATTERN.matcher(command);
        Matcher updateMatcher = UPDATE_SUBSCRIPTION_COMMAND_PATTERN.matcher(command);
        Matcher updateDateEndMatcher = UPDATE_DATE_END_PATTERN.matcher(command);

        String responseText;
        if (setSubCountDateDurationMatcher.find()) {
            Integer count = parseCount(command);
            LocalDate date = parseDate(command);
            Period period = parsePeriod(command);

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

            return getManageSubscriptionsMenu(message, user, true);
        } else if (setSubCountDateMatcher.find()) {
            Integer count = parseCount(command);
            LocalDate date = parseDate(command);

            commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_SUBSCRIPTION_COMMAND
                    + COUNT_ABBR + " " + count
                    + DATE_ABBR + " " + DateUtils.formatDate(date)
                    + DURATION_ABBR);
            responseText = "напиши мне время действия абонемента числом в календарных месяцах (1, 2, 3, 6 и т.п.)";
        } else if (setSubCountMatcher.find()) {
            Integer count = parseCount(command);

            commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_SUBSCRIPTION_COMMAND
                    + COUNT_ABBR + " " + count
                    + DATE_ABBR);
            responseText = "Напиши мне дату начала действия абонемента в формате ДД.ММ.ГГГГ, " +
                    "например: <code>" + DateUtils.formatDate(LocalDate.now()) + "</code>";
        } else if (setSubMatcher.find()) {
            commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_SUBSCRIPTION_COMMAND
                    + COUNT_ABBR);
            responseText = "Напиши мне число тренировок в абонементе";
        } else if (setDeleteMatcher.find()) {
            long subscriptionId = parseId(command, DELETE_SUBSCRIPTION_COMMAND.length());

            TrainSubscription trainSubscription = trainSubscriptionService.get(subscriptionId, user);
            if (trainSubscription == null) {
                return null;
            }

            trainSubscription.setActive(false);
            trainSubscriptionService.save(trainSubscription);
            return getManageSubscriptionsMenu(message, user, false);
        } else if (updateDateEndMatcher.find()) {
            long subscriptionId = parseLong(UPDATE_SUB_PATTERN, command, UPDATE_SUBSCRIPTION_COMMAND);

            TrainSubscription trainSubscription = trainSubscriptionService.get(subscriptionId, user);
            if (trainSubscription == null) {
                botStats.incrementErrors(message, "Запрошен несуществующий абонемент при обновлении даты окончания");
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            LocalDate newEndDate = parseDate(command);
            if (newEndDate.isBefore(trainSubscription.getStartDate())) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            trainSubscription.setPeriod(Period.between(trainSubscription.getStartDate(), newEndDate));
            trainSubscriptionService.save(trainSubscription);

            return getManageSubscriptionsMenu(message, user, true);
        } else if (updateMatcher.find()) {
            long subscriptionId = parseId(command, DELETE_SUBSCRIPTION_COMMAND.length());

            TrainSubscription trainSubscription = trainSubscriptionService.get(subscriptionId, user);
            if (trainSubscription == null) {
                botStats.incrementErrors(message, "Запрошен несуществующий абонемент при удалении абонемента");
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            commandWaitingService.add(chat, user, Set.class, CALLBACK_UPDATE_SUBSCRIPTION_COMMAND + subscriptionId
                    + DATE_ABBR);
            responseText = "Напиши мне дату окончания действия абонемента в формате ДД.ММ.ГГГГ, " +
                    "например: <code>" + DateUtils.formatDate(LocalDate.now()) + "</code>";
        } else {
            return getManageSubscriptionsMenu(message, user, false);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private PartialBotApiMethod<?> getManageSubscriptionsMenu(Message message, User user, boolean newMessage) {
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
                InlineKeyboardButton subscriptionDeleteButton = new InlineKeyboardButton();
                subscriptionDeleteButton.setText(Emoji.DELETE.getEmoji() + subscription.getStartDate() + " " + subscription.getCount());
                subscriptionDeleteButton.setCallbackData(CALLBACK_DELETE_SUBSCRIPTION_COMMAND + subscription.getId());
                buttonRow.add(subscriptionDeleteButton);

                InlineKeyboardButton subscriptionUpdateButton = new InlineKeyboardButton();
                subscriptionUpdateButton.setText(Emoji.GEAR.getEmoji() + formatDate(subscription.getStartDate().plus(subscription.getPeriod())));
                subscriptionUpdateButton.setCallbackData(CALLBACK_UPDATE_SUBSCRIPTION_COMMAND + subscription.getId());
                buttonRow.add(subscriptionUpdateButton);

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

        Matcher setTrainingMatcher = SET_TRAINING_PATTERN.matcher(command);
        Matcher setTrainingAddMatcher = SET_TRAINING_ADD_PATTERN.matcher(command);
        Matcher setTrainingAddTimeEndMatcher = SET_TRAINING_ADD_TIME_END_PATTERN.matcher(command);
        Matcher setTrainingAddTimeCostMatcher = SET_TRAINING_ADD_TIME_COST_PATTERN.matcher(command);
        Matcher setTrainingAddTimeCostNameMatcher = SET_TRAINING_ADD_TIME_COST_NAME_PATTERN.matcher(command);
        Matcher setTrainingDeleteMatcher = SET_TRAINING_DELETE_PATTERN.matcher(command);

        String responseText;
        if (setTrainingAddTimeCostNameMatcher.find()) {
            LocalTime time = parseTime(SET_TIME_COMMAND_PATTERN, command, TIME_ABBR);
            LocalTime timeEnd = parseTime(SET_TIME_END_COMMAND_PATTERN, command, TIME_END_ABBR);
            Float cost = parseCost(command);
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
            LocalTime time = parseTime(SET_TIME_COMMAND_PATTERN, command, TIME_ABBR);
            LocalTime timeEnd = parseTime(SET_TIME_END_COMMAND_PATTERN, command, TIME_END_ABBR);
            Float cost = parseCost(command);

            responseText = "Напиши мне наименование тренировки";
            commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_TRAINING_COMMAND
                    + TIME_ABBR + " " + formatShortTime(time)
                    + TIME_END_ABBR + " " + formatShortTime(timeEnd)
                    + COST_ABBR + " " + cost
                    + NAME_ABBR);
        } else if (setTrainingAddTimeEndMatcher.find()) {
            LocalTime time = parseTime(SET_TIME_COMMAND_PATTERN, command, TIME_ABBR);
            LocalTime timeEnd = parseTime(SET_TIME_END_COMMAND_PATTERN, command, TIME_END_ABBR);

            commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_TRAINING_COMMAND
                    + TIME_ABBR + " " + formatShortTime(time)
                    + TIME_END_ABBR + " " + formatShortTime(timeEnd)
                    + COST_ABBR);
            responseText = "Напиши мне стоимость тренировки (1 — одно занятие, 0,5 — половина занятия, 2 и т.п.)";
        } else if (setTrainingAddMatcher.find()) {
            LocalTime time = parseTime(SET_TIME_COMMAND_PATTERN, command, TIME_ABBR);

            responseText = "Напиши мне время окончания тренировки в формате ЧЧ:ММ. " +
                    "Например: <code>" + formatShortTime(time.plusHours(1)) + "</code>";
            commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_TRAINING_COMMAND
                    + TIME_ABBR + " " + formatShortTime(time)
                    + TIME_END_ABBR);
        } else if (setTrainingMatcher.find()) {
            LocalTime localTimeNow = LocalTime.now();
            responseText = "Напиши мне время начала тренировки в формате ЧЧ:ММ. " +
                    "Например: <code>" + formatShortTime(localTimeNow.minusMinutes(localTimeNow.getMinute())) + "</code>";
            commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_TRAINING_COMMAND
                    + TIME_ABBR);
        } else if (setTrainingDeleteMatcher.find()) {
            long trainingId = parseId(command, DELETE_TRAINING_COMMAND.length());

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
                trainingButton.setText(Emoji.DELETE.getEmoji() + training.getName() + " " + formatShortTime(training.getTimeStart()) + " (" + training.getCost() + ")");
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

        Matcher setScheduleSelectWeekDayMatcher = SET_SCHEDULE_SELECT_WEEK_DAY_PATTERN.matcher(command);
        Matcher setScheduleSelectWeekDayAddTrainingMatcher = SET_SCHEDULE_SELECT_WEEK_DAY_ADD_TRAINING_PATTERN.matcher(command);
        Matcher setScheduleSelectWeekDayRemoveTrainingMatcher = SET_SCHEDULE_SELECT_WEEK_DAY_REMOVE_TRAINING_PATTERN.matcher(command);

        if (STOP_SCHEDULE_COMMAND.equals(command)) {
            if (trainingStoppedService.isStopped(user)) {
                trainingStoppedService.start(user);
            } else {
                trainingStoppedService.stop(user);
            }
            return getManageScheduleMenu(message, user);
        } else if (setScheduleSelectWeekDayAddTrainingMatcher.find()) {
            DayOfWeek dayOfWeek = parseDayOfWeek(command);
            Long trainingId = parseLong(ADD_TRAINING_ENTITY_COMMAND_PATTERN, command, ADD_TRAINING_ABBR);
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
            DayOfWeek dayOfWeek = parseDayOfWeek(command);
            Long trainingId = parseLong(REMOVE_TRAINING_ENTITY_COMMAND_PATTERN, command, REMOVE_TRAINING_ABBR);
            Training training = new Training().setId(trainingId);

            TrainingScheduled trainingScheduled = trainingScheduledService.get(user, dayOfWeek, training);
            if (trainingScheduled == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            trainingScheduledService.remove(trainingScheduled);

            return getManageScheduleByDayOfWeekMenu(message, user, dayOfWeek);
        } else if (setScheduleSelectWeekDayMatcher.find()) {
            DayOfWeek dayOfWeek = parseDayOfWeek(command);
            return getManageScheduleByDayOfWeekMenu(message, user, dayOfWeek);
        } else {
            return getManageScheduleMenu(message, user);
        }
    }

    private EditMessageText getManageScheduleMenu(Message message, User user) {
        StringBuilder buf = new StringBuilder("<b>Текущие тренировки:</b>\n");

        boolean scheduleStopped = trainingStoppedService.isStopped(user);
        if (scheduleStopped) {
            buf.append("<u>РАСПИСАНИЕ ОСТАНОВЛЕНО</u>\n\n");
        }

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> daysOfWeekRow = new ArrayList<>();

        List<TrainingScheduled> trainingScheduledList = trainingScheduledService.get(user);
        if (trainingScheduledList.isEmpty()) {
            buf.append("отсутствуют");
        } else {
            Arrays.stream(DayOfWeek.values()).forEach(dayOfWeek -> {
                buf.append("<b>").append(dayOfWeek.getDisplayName(TextStyle.FULL, LOCALE)).append("</b>\n");
                trainingScheduledList
                        .stream()
                        .filter(trainingScheduled -> dayOfWeek.equals(trainingScheduled.getDayOfWeek()))
                        .forEach(trainingScheduled -> buf.append(trainingScheduled.getTraining().getTimeStart()).append(" — ")
                                .append(trainingScheduled.getTraining().getName()).append(" (").append(trainingScheduled.getTraining().getCost()).append(")\n"));
                buf.append(BORDER);
            });
        }

        Arrays.stream(DayOfWeek.values()).forEach(dayOfWeek -> {
            InlineKeyboardButton dayOfWeekButton = new InlineKeyboardButton();
            dayOfWeekButton.setText(dayOfWeek.getDisplayName(TextStyle.SHORT, LOCALE));
            dayOfWeekButton.setCallbackData(CALLBACK_SELECT_DAY_SCHEDULE_COMMAND + dayOfWeek.getValue());
            daysOfWeekRow.add(dayOfWeekButton);
        });

        String caption;
        if (scheduleStopped) {
            caption = Emoji.CHECK_MARK_BUTTON.getEmoji() + "Запустить расписание";
        } else {
            caption = Emoji.NO_ENTRY_SIGN.getEmoji() + "Остановить расписание";
        }
        List<InlineKeyboardButton> stopRow = new ArrayList<>();
        InlineKeyboardButton stopButton = new InlineKeyboardButton();
        stopButton.setText(caption);
        stopButton.setCallbackData(CALLBACK_STOP_SCHEDULE_COMMAND);
        stopRow.add(stopButton);

        rows.add(daysOfWeekRow);
        rows.add(stopRow);

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
        StringBuilder buf = new StringBuilder("<b>" + dayOfWeek.getDisplayName(TextStyle.FULL, LOCALE) + "</b>\n");
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
                String caption = training.getTimeStart() + " — " + training.getName() + " (" + training.getCost() + ")";

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
            addButton.setText(Emoji.NEW.getEmoji() + training.getTimeStart() + " — " + training.getName() + " (" + training.getCost() + ")");
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

    private Long parseId(String text, int beginIndex) {
        try {
            return Long.parseLong(text.substring(beginIndex));
        } catch (NumberFormatException e) {
            botStats.incrementErrors(text, e, "Не удалось распарсить идентификатор");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

    private Integer parseCount(String text) {
        Matcher matcher = TrainingSetter.SET_COUNT_PATTERN.matcher(text);
        Integer count = parseFromMatcher(matcher, () -> Integer.parseInt(text.substring(matcher.start() + TrainingSetter.COUNT_ABBR.length() + 1, matcher.end())));

        if (count < 1) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return count;
    }

    private Long parseLong(Pattern pattern, String text, String abbr) {
        Matcher matcher = pattern.matcher(text);
        return parseFromMatcher(matcher, () -> Long.parseLong(text.substring(matcher.start() + abbr.length(), matcher.end())));
    }

    private Float parseCost(String text) {
        Matcher matcher = TrainingSetter.SET_COST_COMMAND_PATTERN.matcher(text);
        Float cost = parseFromMatcher(matcher, () -> Float.parseFloat(text.substring(matcher.start() + TrainingSetter.COST_ABBR.length() + 1, matcher.end())));

        if (cost < 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return cost;
    }

    private LocalDate parseDate(String text) {
        Matcher matcher = TrainingSetter.SET_DATE_PATTERN.matcher(text);
        return parseFromMatcher(matcher, () -> LocalDate.parse(text.substring(matcher.start() + TrainingSetter.DATE_ABBR.length() + 1, matcher.end()), dateFormatter));
    }

    private LocalTime parseTime(Pattern pattern, String text, String abbr) {
        Matcher matcher = pattern.matcher(text);
        return parseFromMatcher(matcher, () -> LocalTime.parse(text.substring(matcher.start() + abbr.length() + 1, matcher.end()), timeShortFormatter));
    }

    private Period parsePeriod(String text) {
        Matcher matcher = TrainingSetter.SET_DURATION_PATTERN.matcher(text);
        return parseFromMatcher(matcher, () -> Period.ofMonths(Integer.parseInt(text.substring(matcher.start() + TrainingSetter.DURATION_ABBR.length() + 1, matcher.end()))));
    }

    private DayOfWeek parseDayOfWeek(String text) {
        Matcher matcher = TrainingSetter.SELECT_DAY_OF_WEEK_PATTERN.matcher(text);
        return parseFromMatcher(matcher, () -> DayOfWeek.of(Integer.parseInt(text.substring(matcher.start() + TrainingSetter.DAY_WEEK_ABBR.length(), matcher.end()))));
    }

    private <T> T parseFromMatcher(Matcher matcher, Supplier<T> parse) {
        if (matcher.find()) {
            try {
                return parse.get();
            } catch (Exception e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        } else {
            botStats.incrementErrors(matcher, "Не удалось распарсить" + parse.toString());
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }
}
