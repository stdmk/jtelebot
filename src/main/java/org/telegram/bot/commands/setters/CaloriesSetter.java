package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.UserCaloriesTarget;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.calories.UserCaloriesTargetService;

import javax.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BiFunction;

@RequiredArgsConstructor
@Component
public class CaloriesSetter implements Setter<BotResponse> {

    private static final String CALLBACK_COMMAND = "${setter.command} ";
    private static final String EMPTY_CALORIES_COMMAND = "calories";
    private static final String UPDATE_CALORIES_COMMAND = EMPTY_CALORIES_COMMAND + " ${setter.calories.update}";
    private static final String SET_CALORIES_TARGET_COMMAND = EMPTY_CALORIES_COMMAND + "kcal";
    private static final String CALLBACK_SET_CALORIES_COMMAND = CALLBACK_COMMAND + SET_CALORIES_TARGET_COMMAND;
    private static final String SET_PROTEINS_TARGET_COMMAND = EMPTY_CALORIES_COMMAND + "pr";
    private static final String CALLBACK_SET_PROTEINS_TARGET_COMMAND = CALLBACK_COMMAND + SET_PROTEINS_TARGET_COMMAND;
    private static final String SET_FATS_TARGET_COMMAND = EMPTY_CALORIES_COMMAND + "ft";
    private static final String CALLBACK_SET_FATS_TARGET_COMMAND = CALLBACK_COMMAND + SET_FATS_TARGET_COMMAND;
    private static final String SET_CARBS_TARGET_COMMAND = EMPTY_CALORIES_COMMAND + "cr";
    private static final String CALLBACK_SET_CARBS_TARGET_COMMAND = CALLBACK_COMMAND + SET_CARBS_TARGET_COMMAND;

    private static final DecimalFormat DF = new DecimalFormat("#.#");

    private final Set<String> emptyCaloriesCommands = new HashSet<>();
    private final Set<String> updateCaloriesCommands = new HashSet<>();

    private final InternationalizationService internationalizationService;
    private final UserCaloriesTargetService userCaloriesTargetService;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;

    @PostConstruct
    private void postConstruct() {
        emptyCaloriesCommands.addAll(internationalizationService.getAllTranslations("setter.calories.emptycommand"));
        updateCaloriesCommands.addAll(internationalizationService.internationalize(UPDATE_CALORIES_COMMAND));
    }

    @Override
    public boolean canProcessed(String command) {
        return emptyCaloriesCommands.stream().anyMatch(command::startsWith);
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FAMILIAR;
    }

    @Override
    public BotResponse set(BotRequest request, String commandText) {
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();
        String lowerCaseCommandText = commandText.toLowerCase(Locale.ROOT);

        if (message.isCallback()) {
            if (emptyCaloriesCommands.contains(lowerCaseCommandText) || updateCaloriesCommands.contains(lowerCaseCommandText)) {
                return getMainKeyboard(message, user, false);
            } else if (lowerCaseCommandText.startsWith(SET_CALORIES_TARGET_COMMAND)) {
                return setCaloriesTargetByCallback(message, chat, user);
            } else if (lowerCaseCommandText.startsWith(SET_PROTEINS_TARGET_COMMAND)) {
                return setProteinsTargetByCallback(message, chat, user);
            } else if (lowerCaseCommandText.startsWith(SET_FATS_TARGET_COMMAND)) {
                return setFatsTargetByCallback(message, chat, user);
            } else if (lowerCaseCommandText.startsWith(SET_CARBS_TARGET_COMMAND)) {
                return setCarbsTargetByCallback(message, chat, user);
            }
        }

        if (lowerCaseCommandText.startsWith(SET_CALORIES_TARGET_COMMAND)) {
            return setCaloriesTarget(message, chat, user, commandText);
        } else if (lowerCaseCommandText.startsWith(SET_PROTEINS_TARGET_COMMAND)) {
            return setProteinsTarget(message, chat, user, commandText);
        } else if (lowerCaseCommandText.startsWith(SET_FATS_TARGET_COMMAND)) {
            return setFatsTarget(message, chat, user, commandText);
        } else if (lowerCaseCommandText.startsWith(SET_CARBS_TARGET_COMMAND)) {
            return setCarbsTarget(message, chat, user, commandText);
        }

        return getMainKeyboard(message, user, true);
    }

    private BotResponse setCaloriesTargetByCallback(Message message, Chat chat, User user) {
        return setCommandWaiting(message, chat, user, CALLBACK_SET_CALORIES_COMMAND, "${setter.calories.setcalorieshelp}");
    }

    private BotResponse setProteinsTargetByCallback(Message message, Chat chat, User user) {
        return setCommandWaiting(message, chat, user, CALLBACK_SET_PROTEINS_TARGET_COMMAND, "${setter.calories.setproteinshelp}");
    }

    private BotResponse setFatsTargetByCallback(Message message, Chat chat, User user) {
        return setCommandWaiting(message, chat, user, CALLBACK_SET_FATS_TARGET_COMMAND, "${setter.calories.setfatshelp}");
    }

    private BotResponse setCarbsTargetByCallback(Message message, Chat chat, User user) {
        return setCommandWaiting(message, chat, user, CALLBACK_SET_CARBS_TARGET_COMMAND, "${setter.calories.setcarbshelp}");
    }

    private BotResponse setCommandWaiting(Message message, Chat chat, User user, String callback, String messageText) {
        commandWaitingService.add(chat, user, org.telegram.bot.commands.Set.class, callback);
        return new EditResponse(message)
                .setText(messageText)
                .setResponseSettings(FormattingStyle.HTML);
    }

    private BotResponse setCaloriesTarget(Message message, Chat chat, User user, String command) {
        double value = parseValue(command.substring(SET_CALORIES_TARGET_COMMAND.length()));
        return setTarget(message, chat, user, value, UserCaloriesTarget::setCalories);
    }

    private BotResponse setProteinsTarget(Message message, Chat chat, User user, String command) {
        double value = parseValue(command.substring(SET_PROTEINS_TARGET_COMMAND.length()));
        return setTarget(message, chat, user, value, UserCaloriesTarget::setProteins);
    }

    private BotResponse setFatsTarget(Message message, Chat chat, User user, String command) {
        double value = parseValue(command.substring(SET_FATS_TARGET_COMMAND.length()));
        return setTarget(message, chat, user, value, UserCaloriesTarget::setFats);
    }

    private BotResponse setCarbsTarget(Message message, Chat chat, User user, String command) {
        double value = parseValue(command.substring(SET_CARBS_TARGET_COMMAND.length()));
        return setTarget(message, chat, user, value, UserCaloriesTarget::setCarbs);
    }

    private BotResponse setTarget(Message message, Chat chat, User user, double value, BiFunction<UserCaloriesTarget, Double, UserCaloriesTarget> setter) {
        commandWaitingService.remove(chat, user);

        UserCaloriesTarget userCaloriesTarget = userCaloriesTargetService.get(user);
        if (userCaloriesTarget == null) {
            userCaloriesTarget = new UserCaloriesTarget().setUser(user);
        }

        userCaloriesTargetService.save(setter.apply(userCaloriesTarget, value));

        return getMainKeyboard(message, user, true);
    }

    private double parseValue(String data) {
        try {
            return Double.parseDouble(data);
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private BotResponse getMainKeyboard(Message message, User user, boolean newMessage) {
        String responseText;

        UserCaloriesTarget userCaloriesTarget = userCaloriesTargetService.get(user);
        if (userCaloriesTarget == null) {
            responseText = "${setter.calories.targetsnotset}";
        } else {
            String caloricTarget = "";
            if (userCaloriesTarget.getCalories() != null) {
                caloricTarget = "${setter.calories.calories}: <b>" + DF.format(userCaloriesTarget.getCalories()) + "</b> ${setter.calories.caloriessymbol}.\n";
            }
            String proteinsTarget = "";
            if (userCaloriesTarget.getProteins() != null) {
                proteinsTarget = "${setter.calories.proteins}: <b>" + DF.format(userCaloriesTarget.getProteins()) + "</b> ${setter.calories.gramssymbol}.\n";
            }
            String fatsTarget = "";
            if (userCaloriesTarget.getFats() != null) {
                fatsTarget = "${setter.calories.fats}: <b>" + DF.format(userCaloriesTarget.getFats()) + "</b> ${setter.calories.gramssymbol}.\n";
            }
            String carbsTarget = "";
            if (userCaloriesTarget.getCarbs() != null) {
                carbsTarget = "${setter.calories.carbs}: <b>" + DF.format(userCaloriesTarget.getCarbs()) + "</b> ${setter.calories.gramssymbol}.\n";
            }

            responseText = "${setter.calories.currenttargets}:\n" + caloricTarget + proteinsTarget + fatsTarget + carbsTarget;
        }

        if (newMessage) {
            return new TextResponse(message)
                    .setText(responseText)
                    .setKeyboard(new Keyboard(prepareMainKeyboard()))
                    .setResponseSettings(FormattingStyle.HTML);
        }

        return new EditResponse(message)
                .setText(responseText)
                .setKeyboard(new Keyboard(prepareMainKeyboard()))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private List<List<KeyboardButton>> prepareMainKeyboard() {
        return new ArrayList<>(List.of(
                List.of(new KeyboardButton()
                        .setName(Emoji.FIRE.getSymbol() + "${setter.calories.button.calories}")
                        .setCallback(CALLBACK_SET_CALORIES_COMMAND)),
                List.of(new KeyboardButton()
                        .setName(Emoji.CHICKEN.getSymbol() + "${setter.calories.button.proteins}")
                        .setCallback(CALLBACK_SET_PROTEINS_TARGET_COMMAND)),
                List.of(new KeyboardButton()
                        .setName(Emoji.PIG.getSymbol() + "${setter.calories.button.fats}")
                        .setCallback(CALLBACK_SET_FATS_TARGET_COMMAND)),
                List.of(new KeyboardButton()
                        .setName(Emoji.BREAD.getSymbol() + "${setter.calories.button.carbs}")
                        .setCallback(CALLBACK_SET_CARBS_TARGET_COMMAND)),
                List.of(new KeyboardButton()
                        .setName(Emoji.UPDATE.getSymbol() + "${setter.calories.button.update}")
                        .setCallback(CALLBACK_COMMAND + UPDATE_CALORIES_COMMAND)),
                List.of(new KeyboardButton()
                        .setName(Emoji.BACK.getSymbol() + "${setter.calories.button.settings}")
                        .setCallback(CALLBACK_COMMAND + "back"))));
    }

}
